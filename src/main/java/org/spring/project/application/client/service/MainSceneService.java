package org.spring.project.application.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.spring.project.application.client.Dto.Game;
import org.spring.project.application.client.Dto.GameFileDto;
import org.spring.project.application.client.Dto.GameUpdateNewsDto;
import org.spring.project.application.client.elements.LibraryListElement;
import org.spring.project.application.client.elements.UpdateNewsFlowPane;
import org.spring.project.application.client.properties.*;
import org.spring.project.application.client.scenes.MainScene;
import org.spring.project.application.client.undoableEvent.EventType;
import org.spring.project.application.client.undoableEvent.UndoableEvent;
import org.spring.project.application.client.undoableEvent.UrlEvent;
import org.spring.project.application.client.undoableEvent.libraryEvent.LibraryListEvent;
import org.spring.project.application.client.undoableEvent.libraryEvent.LibraryMainPageEvent;
import org.spring.project.application.client.userLibrary.UserLibrary;
import org.spring.project.application.client.utils.ApplicationUtils;
import org.spring.project.application.client.webclient.RequestBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.spring.project.application.client.application.FxApplication.primaryStage;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainSceneService implements SceneService {

    private final MainScene mainScene;
    private final FolderProperties folderProperties;
    private final ResourceProperties resourceProperties;
    private final AuthenticationSceneService authenticationSceneService;
    private final ApplicationUtils applicationUtils;
    private final RequestBuilder requestBuilder;
    private final UserLibrary userLibrary;
    private final KeyProperties keyProperties;
    private final GameProperties gameProperties;
    private final UrlProperties urlProperties;

    private final List<UndoableEvent> eventList = new ArrayList<>();
    private int activeEvent = -1;
    private UndoableEvent storeLastEvent;
    private UndoableEvent libraryLastEvent;
    private UndoableEvent communityLastEvent;
    private UndoableEvent profileLastEvent;

    @PostConstruct
    protected void init() {

//        Scene element listeners - start
        mainScene.getGames_list_listView().getSelectionModel().selectedIndexProperty().addListener((obsVal, oldVal, newVal) -> {
            if (!newVal.equals(-1) && !newVal.equals(oldVal)) {
                String gameTitle = mainScene.getGames_list_listView()
                        .getSelectionModel()
                        .getSelectedItem()
                        .getGame().getTitle();
                userLibrary.getGames().stream().filter(g ->
                        g.getTitle().equals(gameTitle)).findAny().ifPresent(this::getNews);
            }
        });
        mainScene.getSearch_textField().textProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal.equals("")) {
                mainScene.getGames_list_listView().setItems(FXCollections.observableArrayList(
                        userLibrary.getGames().stream().map(LibraryListElement::new).collect(Collectors.toList()))
                        .sorted(Comparator.comparing(libraryListElement -> libraryListElement.getGame().getTitle())));
            } else {
                mainScene.getGames_list_listView().setItems(FXCollections.observableArrayList(
                        userLibrary.getGames().stream().map(LibraryListElement::new)
                                .filter(element ->
                                        element.getGame().getTitle().toLowerCase().contains(newVal.toLowerCase()))
                                .collect(Collectors.toList()))
                        .sorted(Comparator.comparing(libraryListElement -> libraryListElement.getGame().getTitle())));
            }
        });
        mainScene.getToggleGroup().selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            }
        });
        Platform.runLater(() -> {
            mainScene.getWebEngine().getLoadWorker().stateProperty().addListener(
                    (o, old, state) -> {
                        if (state != Worker.State.SUCCEEDED) {
                            return;
                        }
                        setEventToLink();
                    });
        });
//        Scene element listeners - end

//        Scene element events - start
        mainScene.getLogout_menu_item().setOnAction(event -> applicationUtils.changeScene(primaryStage, authenticationSceneService));
        mainScene.getCollapse_button().setOnAction(event -> primaryStage.setIconified(true));
        mainScene.getExpand_button().setOnAction(event -> primaryStage.setMaximized(!primaryStage.isMaximized()));
        mainScene.getClose_button().setOnAction(event -> {
            primaryStage.hide();
            Platform.exit();
        });

        mainScene.getStore_toggleButton().setOnMousePressed(mouseEvent -> {
            if (mainScene.getToggleGroup().getSelectedToggle().equals(mainScene.getStore_toggleButton())) {
                pushEvent(pageRequest(EventType.STORE_EVENT, urlProperties.getStoreUrl()));
            } else {
                if (storeLastEvent == null) {
                    storeLastEvent = pageRequest(EventType.STORE_EVENT, urlProperties.getStoreUrl());
                    pushEvent(storeLastEvent);
                } else {
                    pushEvent(pageRequest(EventType.STORE_EVENT, (String) storeLastEvent.getElement()));
                }
            }
        });
        mainScene.getStore_toggleButton().setOnAction(actionEvent -> {
            mainScene.getMain_borderPane().setLeft(null);
            mainScene.getMain_borderPane().setCenter(mainScene.getWebView());
        });

        mainScene.getLibrary_toggleButton().setOnMousePressed(mouseEvent -> {
            if (mainScene.getToggleGroup().getSelectedToggle().equals(mainScene.getLibrary_toggleButton())) {
                mainScene.getGames_list_listView().getSelectionModel().clearSelection();
                mainScene.getMain_library_center_anchorPane().getChildren().setAll(mainScene.getLibrary_main_page_flowPane());
                pushEvent(new LibraryMainPageEvent(mainScene.getLibrary_main_page_flowPane()));
            } else {
                if (libraryLastEvent == null) {
                    libraryLastEvent = new LibraryMainPageEvent(mainScene.getLibrary_main_page_flowPane());
                }
                pushEvent(libraryLastEvent);
            }
        });
        mainScene.getLibrary_toggleButton().setOnAction(mouseEvent -> {
            mainScene.getMain_borderPane().setLeft(mainScene.getMain_library_left_vBox());
            mainScene.getMain_borderPane().setCenter(mainScene.getMain_library_center_anchorPane());
        });

        mainScene.getCommunity_toggleButton().setOnMousePressed(mouseEvent -> {
            if (mainScene.getToggleGroup().getSelectedToggle().equals(mainScene.getCommunity_toggleButton())) {
                pushEvent(pageRequest(EventType.COMMUNITY_EVENT, urlProperties.getCommunityUrl()));
            } else {
                if (communityLastEvent == null) {
                    communityLastEvent = pageRequest(EventType.COMMUNITY_EVENT, urlProperties.getCommunityUrl());
                    pushEvent(communityLastEvent);
                } else {
                    pushEvent(pageRequest(EventType.COMMUNITY_EVENT, (String) communityLastEvent.getElement()));
                }
            }
        });
        mainScene.getCommunity_toggleButton().setOnAction(actionEvent -> {
            mainScene.getMain_borderPane().setLeft(null);
            mainScene.getMain_borderPane().setCenter(mainScene.getWebView());
        });

        mainScene.getProfile_toggleButton().setOnMousePressed(mouseEvent -> {
            if (mainScene.getToggleGroup().getSelectedToggle().equals(mainScene.getProfile_toggleButton())) {
                pushEvent(pageRequest(EventType.STORE_EVENT, urlProperties.getProfileUrl()));
            } else {
                if (profileLastEvent == null) {
                    profileLastEvent = pageRequest(EventType.PROFILE_EVENT, urlProperties.getProfileUrl());
                    pushEvent(profileLastEvent);
                } else {
                    pushEvent(pageRequest(EventType.PROFILE_EVENT, (String) profileLastEvent.getElement()));
                }
            }
        });
        mainScene.getProfile_toggleButton().setOnAction(actionEvent -> {
            mainScene.getMain_borderPane().setLeft(null);
            mainScene.getMain_borderPane().setCenter(mainScene.getWebView());
        });

        mainScene.getBackward_button().setOnAction(actionEvent -> undo());
        mainScene.getForward_button().setOnAction(actionEvent -> redo());
//        Scene element events - end
    }

    @Override
    public Parent buildScene(Stage stage) {
        mainScene.getMain_borderPane().setTop(mainScene.getMain_top_vBox());
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setWidth(1200);
        stage.setHeight(900);
        stage.centerOnScreen();
        pushEvent(new LibraryMainPageEvent(mainScene.getLibrary_main_page_flowPane()));
        getUserLibrary();
        mainScene.getLibrary_toggleButton().fire();
        return mainScene.getMain_borderPane();
    }

    private UndoableEvent getLast() {
        return eventList.get(eventList.size() - 1);
    }

    private String getDefaultConnectionExceptionPage(String url) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\"\n" +
                "      xmlns=\"http://www.w3.org/1999/xhtml\"" +
                "<head>\n" +
                "    <title>Connection failed</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<p>" + url + "</p>\n" +
                "<p>Нет соединения с сервером</p>\n" +
                "</body>\n" +
                "</html>";
    }

    private EventListener pageEventListener(String url, String method) {
        return event -> {
            String dataAttribute = ((Element) event.getCurrentTarget()).getAttribute("data-attribute");
            requestBuilder
                    .builderRequest(method, url, null, null, true)
                    .bodyToMono(String.class)
                    .onErrorContinue((throwable, o) -> Platform.runLater(() -> mainScene.getWebEngine().loadContent(
                            getDefaultConnectionExceptionPage(url))))
                    .subscribe(page -> {
                        if (dataAttribute != null && dataAttribute.equals("buy")) {
                            getUserLibrary();
                        }
                        Platform.runLater(() ->
                                mainScene.getWebEngine().loadContent(page));
                    });
            pushEvent(new UrlEvent(applicationUtils.checkLinkType(url), url));
            mainScene.getMain_borderPane().setLeft(null);
            mainScene.getMain_borderPane().setCenter(mainScene.getWebView());
        };
    }

//    Доделать поиск в navbar.
//    Иногда приложение разворачивается с меньшим размером при нажатии на понель задач.
//    При уменьшении окна navbar сворачивается и все его элементы пропадают.

    private void setEventToLink() {
        Document doc = mainScene.getWebEngine().getDocument();
        NodeList allPageElements = doc.getElementsByTagName("*");
        for (int i = 0; i < allPageElements.getLength(); i++) {
            Element element = (Element) allPageElements.item(i);
            if (element.getTagName().equals("A") || element.getTagName().equals("FORM")) {
                String reference;
                Node referenceType = element.getAttributes().getNamedItem("href");
                Node actionType = element.getAttributes().getNamedItem("action");
                if (referenceType != null && referenceType.getNodeValue().startsWith("/")) {
                    reference = referenceType.getNodeValue();
                } else if (actionType != null && actionType.getNodeValue().startsWith("/")) {
                    reference = actionType.getNodeValue();
                } else {
                    continue;
                }
                Node methodType = element.getAttributes().getNamedItem("method");
                String method;
                if (methodType != null) {
                    method = methodType.getNodeValue();
                } else {
                    method = urlProperties.getGetMethod();
                }
                ((EventTarget) allPageElements.item(i)).addEventListener("click",
                        pageEventListener(reference, method), false);
            }
        }
    }

    private UndoableEvent pageRequest(EventType mainType, String url) {
        requestBuilder
                .builderRequest(urlProperties.getGetMethod(), url, null, null, true)
                .bodyToMono(String.class)
                .onErrorContinue((throwable, o) -> Platform.runLater(() -> mainScene.getWebEngine().loadContent(
                        getDefaultConnectionExceptionPage(url))))
                .subscribe(page -> Platform.runLater(() -> mainScene.getWebView().getEngine().loadContent(page)));
        return new UrlEvent(mainType, url);
    }

    private void pushEvent(UndoableEvent event) {
        if (!eventList.isEmpty()) {
            UndoableEvent active = eventList.get(activeEvent);
            if (!getLast().equals(active) && !active.equals(event)) {
                eventList.subList(activeEvent + 1, eventList.size()).clear();
            }
            if (getLast().equals(event)) {
                return;
            }
        }
        switch (event.getMainType()) {
            case STORE_EVENT:
                storeLastEvent = event;
                break;
            case LIBRARY_EVENT:
                libraryLastEvent = event;
                break;
            case COMMUNITY_EVENT:
                communityLastEvent = event;
                break;
            case PROFILE_EVENT:
                profileLastEvent = event;
                break;
            default:
                log.error("Ошибка в switch метода push при попытке поместить событие " + event + " в стек");
                return;

        }
        eventList.add(event);
        if (eventList.size() > 30) {
            eventList.subList(0, eventList.size() - 29).clear();
            activeEvent = eventList.indexOf(getLast());
        } else {
            activeEvent++;
        }
    }

    private void undoableHandler(UndoableEvent event) {

        if (event.getType().equals(EventType.URL)) {
            switch (event.getMainType()) {
                case STORE_EVENT:
                    if (!mainScene.getStore_toggleButton().isSelected()) {
                        mainScene.getStore_toggleButton().fire();
                    }
                    storeLastEvent = event;
                    break;
                case COMMUNITY_EVENT:
                    if (!mainScene.getCommunity_toggleButton().isSelected()) {
                        mainScene.getCommunity_toggleButton().fire();
                    }
                    communityLastEvent = event;
                    break;
                case PROFILE_EVENT:
                    if (!mainScene.getProfile_toggleButton().isSelected()) {
                        mainScene.getProfile_toggleButton().fire();
                    }
                    profileLastEvent = event;
                    break;
                default:
                    log.error("Ошибка в методе undoableHandler: неверно указан основной тип URL евента");
                    return;
            }
            pageRequest(event.getMainType(), (String) event.getElement());
            return;
        }
        if (event.getMainType().equals(EventType.LIBRARY_EVENT)) {
            if (!mainScene.getLibrary_toggleButton().isSelected()) {
                mainScene.getLibrary_toggleButton().fire();
            }
            if (event.getType().equals(EventType.LIBRARY_MAIN_PAGE)) {
                mainScene.getGames_list_listView().getSelectionModel().clearSelection();
                mainScene.getMain_library_center_anchorPane().getChildren().setAll((FlowPane) event.getElement());
            }
            if (event.getType().equals(EventType.LIBRARY_LIST)) {
                mainScene.getGames_list_listView().getSelectionModel().select((LibraryListElement) event.getElement());
            }
            libraryLastEvent = event;
        }
    }

    private void undo() {
        if (!eventList.isEmpty() && activeEvent > 0) {
            UndoableEvent previous = eventList.get(--activeEvent);
            undoableHandler(previous);
        }
    }

    private void redo() {
        if (!eventList.isEmpty() && activeEvent + 1 < eventList.size()) {
            UndoableEvent next = eventList.get(++activeEvent);
            undoableHandler(next);
        }
    }

    private void offlineLibrary() {
        File file = new File(folderProperties.getLibraryFolder());
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            userLibrary.setGames((List<Game>) objectInputStream.readObject());
        } catch (FileNotFoundException e) {
            log.error("Файл библиотеки не найден");
        } catch (ClassNotFoundException e) {
            log.error("Не удалось преобразовать данные из файла библиотеки");
        } catch (IOException e) {
            log.error("Ошибка при загрузке библиотеки из файла");
        }
    }

    private void getUserLibrary() {

        requestBuilder.builderRequest(urlProperties.getGetMethod(),
                urlProperties.getUserUrl() + urlProperties.getLibraryUrl(), null, null, true)
                .bodyToMono(new ParameterizedTypeReference<List<Game>>() {
                })
                .onErrorContinue((throwable, o) -> {
                    log.error("Ошибка запроса библиотеки пользователя");
                    offlineLibrary();
                    buildLibraryContent();
                    getLibraryMainPage();
                })
                .subscribe(library -> {
                    userLibrary.setGames(library);
                    userLibrary.getGames().sort(Comparator.comparing(Game::getTitle));
                    buildLibraryContent();
                    getLibraryMainPage();
                });
    }

    private void getLibraryMainPage() {
        Platform.runLater(() -> mainScene.getLibrary_main_page_flowPane().getChildren().clear());
        if (!userLibrary.getGames().isEmpty()) {
            userLibrary.getGames().forEach(game -> {
                File libraryMainPageImage = new File(
                        folderProperties.getResourcesFolder() +
                                game.getName() +
                                File.separator +
                                resourceProperties.getLibraryMainImage() +
                                File.separator +
                                resourceProperties.getLibraryMainImage() + resourceProperties.getImageFormat());

                ImageView image = new ImageView();
                image.setFitWidth(300);
                image.setFitHeight(200);
                image.setOnMouseClicked(event -> mainScene.getGames_list_listView().getItems().stream().filter(
                        libraryListElement -> libraryListElement.getGame().getTitle().equals(game.getTitle()))
                        .findFirst().ifPresent(listElement ->
                                mainScene.getGames_list_listView().getSelectionModel().select(listElement)));

                try (FileInputStream fis = new FileInputStream(libraryMainPageImage.getPath())) {
                    image.setImage(new Image(fis));
                    Platform.runLater(() -> mainScene.getLibrary_main_page_flowPane().getChildren().add(image));
                } catch (IOException e) {
                    log.error("Не удалось загрузить картинку главной страницы библиотеки для игры {} из файла",
                            game.getTitle());
                    DataBufferUtils
                            .join(requestBuilder.builderRequest(urlProperties.getGetMethod(),
                                    urlProperties.getGamesUrl() + "/" + game.getName() +
                                            urlProperties.getLibraryMainImage(), null, null, true)
                                    .bodyToFlux(DataBuffer.class))
                            .map(applicationUtils::fluxDataBufferToByteArray)
                            .onErrorContinue((throwable, o) ->
                                    Platform.runLater(() -> {
                                        log.error("Не удалось загрузить картинку главной страницы библиотеки для игры {}",
                                                game.getTitle());
                                        image.setStyle("-fx-background-color: #ffa07a");
                                        mainScene.getLibrary_main_page_flowPane().getChildren().add(image);
                                    }))
                            .subscribe(libraryMainImageBytes -> {
                                try (ByteArrayInputStream imageStream =
                                             new ByteArrayInputStream(libraryMainImageBytes)) {

                                    image.setImage(new Image(imageStream));
                                    Platform.runLater(() -> mainScene.getLibrary_main_page_flowPane().getChildren().add(image));
                                    FileUtils.forceMkdir(libraryMainPageImage.getParentFile());
                                    try (FileOutputStream fos = new FileOutputStream(libraryMainPageImage)) {
                                        fos.write(libraryMainImageBytes);
                                    }
                                } catch (IOException ex) {
                                    log.error("Не удалось сохранить картинку главной страницы библиотеки для игры {} в файл",
                                            game.getTitle());
                                }
                            });
                }
            });
        }
    }

    private void setCellFactory() {
        Platform.runLater(() ->
                mainScene.getGames_list_listView().setCellFactory(listView -> {
                    ListCell<LibraryListElement> cell = new ListCell<>() {
                        private final ImageView gameLogo = new ImageView();

                        @Override
                        protected void updateItem(LibraryListElement libraryElement, boolean empty) {
                            super.updateItem(libraryElement, empty);
                            if (empty) {
                                setText(null);
                                setGraphic(null);
                            } else {
                                setText(libraryElement.getGame().getTitle());
                                gameLogo.setFitWidth(20);
                                gameLogo.setFitHeight(20);
                                gameLogo.setImage(libraryElement.getLogo());
                                setGraphic(gameLogo);
                            }
                        }
                    };
                    cell.setOnMousePressed(event -> {
                        if (!cell.isEmpty()) {
                            if (!((getLast()).getElement().equals(cell.getItem()))) {
                                pushEvent(new LibraryListEvent(cell.getItem()));
                            }
                        }
                    });
                    return cell;
                }));
    }

    private void buildLibraryContent() {

        Platform.runLater(() -> mainScene.getGames_list_listView().setItems(FXCollections.observableArrayList(
                userLibrary.getGames().stream().map(game -> {
                    LibraryListElement element = new LibraryListElement(game);
                    File libraryLogoImage = new File(folderProperties.getResourcesFolder() +
                            game.getName() +
                            File.separator +
                            resourceProperties.getLibraryLogo() +
                            File.separator +
                            resourceProperties.getLibraryLogo() + resourceProperties.getImageFormat());
                    try (FileInputStream fis = new FileInputStream(libraryLogoImage)) {
                        element.setLogo(new Image(fis));
                        setCellFactory();
                    } catch (IOException e) {
                        log.error("Не удалось загрузить логотип библиотеки для игры {} из файла", game.getTitle());
                        DataBufferUtils.
                                join(requestBuilder.builderRequest(urlProperties.getGetMethod(),
                                        urlProperties.getGamesUrl() + "/" + game.getName() +
                                                urlProperties.getLibraryLogo(), null, null, true)
                                        .bodyToFlux(DataBuffer.class))
                                .map(applicationUtils::fluxDataBufferToByteArray)
                                .onErrorContinue((throwable, o) -> {
                                    log.error("Не удалось загрузить логотип библиотеки для игры {}",
                                            game.getTitle());
                                    setCellFactory();
                                })
                                .subscribe(libraryLogoBytes -> {
                                    try (ByteArrayInputStream imageStream =
                                                 new ByteArrayInputStream(libraryLogoBytes)) {

                                        element.setLogo(new Image(imageStream));
                                        FileUtils.forceMkdir(libraryLogoImage.getParentFile());
                                        try (FileOutputStream fos = new FileOutputStream(libraryLogoImage)) {
                                            fos.write(libraryLogoBytes);
                                        }
                                    } catch (IOException ex) {
                                        log.error("Не удалось сохранить логотип библиотеки для игры {} в файл",
                                                game.getTitle());
                                    }
                                    setCellFactory();
                                });
                    }
                    return element;
                }).collect(Collectors.toList()))));
    }

    private void getNews(Game game) {
        if (mainScene.getMain_library_center_anchorPane().getChildren().contains(mainScene.getLibrary_main_page_flowPane())) {
            mainScene.getMain_library_center_anchorPane().getChildren().setAll(mainScene.getMain_library_center_scrollPane());
        }
        mainScene.getUpdate_news_vBox().getChildren().clear();

        requestBuilder.builderRequest(urlProperties.getGetMethod(),
                urlProperties.getGamesUrl() + "/" + game.getName() + urlProperties.getGameActualSize(),
                null, null, true)
                .toBodilessEntity()
                .onErrorContinue((throwable, o) -> {
                    log.error("Не удалось получить актуальный размер игры {}", game.getTitle());
                    gameStartButtonBuilder(game, 0);
                })
                .subscribe(responseEntity -> {
                    long gameSize = 0;
                    if (responseEntity.getStatusCode().equals(OK)) {
                        String sizeHeader = responseEntity.getHeaders().getFirst(keyProperties.getGameSize());
                        if (sizeHeader != null && sizeHeader.matches("^[0-9]+$")) {
                            gameSize = Long.parseLong(sizeHeader);
                            game.setDefaultGameSize(gameSize);
                        }
                        gameStartButtonBuilder(game, gameSize);
                    } else {
                        log.error("Не удалось получить актуальный размер игры {}", game.getTitle());
                    }
                });

        File gamePreviewImage = new File(folderProperties.getResourcesFolder() +
                game.getName() +
                File.separator +
                resourceProperties.getNewsPreviewImage() +
                File.separator +
                resourceProperties.getNewsPreviewImage() + resourceProperties.getImageFormat());

        try (FileInputStream fis = new FileInputStream(gamePreviewImage)) {
            mainScene.getGame_preview_image_imageView().setImage(new Image(fis));
        } catch (IOException e) {
            log.error("Не удалось загрузить картинку новостей для игры {} из папки", game.getTitle());
            DataBufferUtils
                    .join(requestBuilder.builderRequest(urlProperties.getGetMethod(),
                            urlProperties.getGamesUrl() + "/" + game.getName() +
                                    urlProperties.getNewsPreviewImage(), null, null, true)
                            .bodyToFlux(DataBuffer.class))
                    .map(applicationUtils::fluxDataBufferToByteArray)
                    .onErrorContinue((throwable, o) -> {
                        try (FileInputStream fis = new FileInputStream(folderProperties.getErrorsFolder() +
                                resourceProperties.getServerErrorImage() + resourceProperties.getImageFormat())) {
                            Platform.runLater(() -> mainScene.getGame_preview_image_imageView().setImage(new Image(fis)));
                        } catch (IOException ex) {
                            log.error("Не удалось найти картинку ошибки новостей для игры {}", game.getTitle());
                            Platform.runLater(() -> {
                                mainScene.getGame_preview_image_imageView().setFitHeight(240);
                                mainScene.getLibrary_center_menu_vBox().setStyle("-fx-background-color: #ffa07a");
                            });
                        }
                    })
                    .subscribe(previewImageBytes -> Platform.runLater(() -> {
                        try (ByteArrayInputStream imageStream =
                                     new ByteArrayInputStream(previewImageBytes)) {

                            mainScene.getGame_preview_image_imageView().setImage(new Image(imageStream));
                            FileUtils.forceMkdir(gamePreviewImage.getParentFile());
                            try (FileOutputStream fos = new FileOutputStream(gamePreviewImage)) {
                                fos.write(previewImageBytes);
                            }
                        } catch (IOException ex) {
                            log.error("Не удалось сохранить картинку новостей для игры {} в файл",
                                    game.getTitle());
                        }
                    }));
        }

        requestBuilder.builderRequest(urlProperties.getGetMethod(),
                urlProperties.getGamesUrl() + "/" + game.getName() + urlProperties.getGameUpdateNews(),
                null, null, true)
                .bodyToMono(new ParameterizedTypeReference<Set<GameUpdateNewsDto>>() {
                })
                .onErrorContinue((throwable, o) -> {
                    log.error("Не удалось загрузить список новостей обновлений для игры {}", game.getTitle());
                })
                .subscribe(gameUpdateNews -> {
                    Flux.fromIterable(gameUpdateNews.stream().map(gameUpdateNewsDto ->
                            DataBufferUtils
                                    .join(requestBuilder.builderRequest(urlProperties.getPostMethod(),
                                            urlProperties.getGamesUrl() + "/" + game.getName() +
                                                    urlProperties.getNewsUpdateImage(), null, gameUpdateNewsDto, true)
                                            .bodyToFlux(DataBuffer.class))
                                    .map(applicationUtils::fluxDataBufferToByteArray)
                                    .onErrorContinue((throwable, o) -> log.error("Не удалось загрузить новости обновлений для игры {}",
                                            game.getTitle()))
                                    .map(patchImageBytes -> {

                                        ImageView update_image = new ImageView();
                                        update_image.setFitWidth(300);
                                        update_image.setFitHeight(200);

                                        Label updateText = new Label(gameUpdateNewsDto.getUpdateText());
                                        updateText.setWrapText(true);
                                        updateText.setPrefWidth(300);
                                        updateText.setPrefHeight(190);

                                        Label date = new Label(gameUpdateNewsDto.getUpdateTime().format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")));
                                        date.setWrapText(true);
                                        date.setPrefWidth(300);
                                        date.setPrefHeight(10);

                                        VBox updateInfo = new VBox(date, updateText);

                                        try (ByteArrayInputStream byteArrayInputStream =
                                                     new ByteArrayInputStream(patchImageBytes)) {
                                            update_image.setImage(new Image(byteArrayInputStream));
                                        } catch (IOException exception) {
                                            log.error("Не удалось загрузить картинку обновления");
                                        }
                                        FlowPane flowPane = new FlowPane(update_image, updateInfo);
                                        return new UpdateNewsFlowPane(flowPane, gameUpdateNewsDto.getUpdateTime());
                                    })).collect(Collectors.toList())
                    )
                            .flatMap(flowPaneMono -> flowPaneMono.map(flowPane -> flowPane)).collectList()
                            .subscribe(updateNewsFlowPanes -> {
                                updateNewsFlowPanes.sort(Comparator.comparing(UpdateNewsFlowPane::getUpdateDate).reversed());
                                for (UpdateNewsFlowPane newsFlowPane : updateNewsFlowPanes) {
                                    Platform.runLater(() -> mainScene.getUpdate_news_vBox().getChildren().add(newsFlowPane.getUpdateFlow()));
                                }
                            });
                });
    }

    private void downloadGameFiles(Game game) {

        var ref = new Object() {
            long gameSize = 0;
            long downloaded = 0;
            long percentDownloaded = 0;
            long alreadyDownloaded = 0;
        };

        String gameDirectoryName = folderProperties.getGamesFolder() + game.getName();
        File gameDirectory = new File(gameDirectoryName);
        if (gameDirectory.exists()) {
            ref.downloaded = FileUtils.sizeOfDirectory(gameDirectory);
        }

        requestBuilder.builderRequest(urlProperties.getGetMethod(),
                urlProperties.getGamesUrl() + "/" + game.getName() + urlProperties.getGameFilesList(),
                null, null, true)
                .toEntity(new ParameterizedTypeReference<String>() {
                })
                .onErrorContinue((throwable, o) -> log.error("Не удалось получить список файлов игры {}", game.getTitle()))
                .subscribe(responseEntity -> {
                    if (responseEntity.getStatusCode().equals(OK)) {
                        String size = responseEntity.getHeaders().getFirst(keyProperties.getGameSize());
                        if (size != null) {
                            ref.gameSize = Long.parseLong(size);
                        }
                        if (responseEntity.getBody() != null) {

                            List<String> gameFilesInfo;
                            try {
                                gameFilesInfo = new ObjectMapper().readValue(responseEntity.getBody(), new TypeReference<>() {
                                });
                            } catch (IOException e) {
                                log.error("Не удалось преобразовать список файлов игры {}", game.getTitle());
                                return;
                            }
                            for (String filePath : gameFilesInfo) {
                                File file = new File(gameDirectoryName + filePath);
                                try {
                                    FileUtils.forceMkdir(file.getParentFile());
                                } catch (IOException e) {
                                    log.error("Не удалось создать директорию игры {}", game.getTitle());
                                    e.printStackTrace();
                                    return;
                                }

                                long alreadyDownloadedFileSize;

                                if (file.exists()) {
                                    alreadyDownloadedFileSize = file.length();
                                } else {
                                    alreadyDownloadedFileSize = 0;
                                }

                                GameFileDto gameFileDto =
                                        new GameFileDto(game.getName(), filePath, alreadyDownloadedFileSize);

                                try {
                                    FileOutputStream fos = new FileOutputStream(file, file.exists()) {
                                        @Override
                                        public void write(byte[] b, int off, int len) throws IOException {
                                            super.write(b, off, len);
                                            if (ref.gameSize != 0) {
                                                ref.downloaded += len;
                                                ref.percentDownloaded = (ref.downloaded * 100) / ref.gameSize;
                                                if (ref.alreadyDownloaded != ref.percentDownloaded &&
                                                        ref.percentDownloaded < 100) {
                                                    log.info("Загружено " + ref.percentDownloaded + "%");
                                                }
                                                ref.alreadyDownloaded = ref.percentDownloaded;
                                            }
                                        }
                                    };

                                    DataBufferUtils
                                            .write(requestBuilder.builderRequest(urlProperties.getPostMethod(),
                                                    urlProperties.getGamesUrl() + "/" + game.getName() +
                                                            urlProperties.getDownloadGameFile(), null, gameFileDto, true)
                                                    .bodyToFlux(DataBuffer.class), fos)
                                            .onErrorContinue((throwable, o) -> log.error("Не удалось загрузить файл {} игры {}",
                                                    file.getName(), gameFileDto.getGameName()))
                                            .map(DataBufferUtils::release)
                                            .then()
                                            .doFinally(signalType -> {
                                                try {
                                                    fos.flush();
                                                    fos.close();
                                                } catch (IOException e) {
                                                    log.error("Не удалось закрыть поток записи к файлу {}",
                                                            gameFileDto.getFilePath());
                                                }
                                                if (ref.downloaded == ref.gameSize) {
                                                    log.info("Загружено 100%");
                                                    gameStartEvent(game);
                                                }
                                                if (file.length() == 0) {
                                                    if (!file.delete()) {
                                                        log.error("Не удалось удалить пустой файл {}", file.getName());
                                                    }
                                                }
                                            })
                                            .subscribe();
                                } catch (IOException e) {
                                    log.error("Ошибка при записи файла {}", gameFileDto.getFilePath());
                                }
                            }
                        }
                    } else {
                        if (responseEntity.getStatusCode().equals(FOUND)) {
                            log.info("Файл уже загружен");
                        } else {
                            log.error("Ошибка при загрузке файла");
                        }
                    }
                });
    }

    private void gameStartButtonBuilder(Game game, long sizeHeader) {
        File directory = new File(folderProperties.getGamesFolder() + game.getName());
        long sizeOfDirectory = 0;
        if (directory.exists()) {
            sizeOfDirectory = FileUtils.sizeOfDirectory(directory);
        }
        if (sizeOfDirectory > 0) {
            long gameSize = ((sizeHeader > 0) ? sizeHeader : game.getDefaultGameSize());
            Process process = game.getProcess();
            if (process != null && process.isAlive()) {
                gameStopEvent(process);
            } else {
                if (sizeOfDirectory != gameSize) {
                    gameDownloadEvent(game, "ПРОДОЛЖИТЬ ЗАГРУЗКУ");
                } else {
                    gameStartEvent(game);
                }
            }
        } else {
            gameDownloadEvent(game, "УСТАНОВИТЬ");
        }
        Platform.runLater(() -> {
            if (!mainScene.getGame_launch_menu_hBox().getChildren().contains(mainScene.getGame_launch_button())) {
                mainScene.getGame_launch_menu_hBox().getChildren().add(mainScene.getGame_launch_button());
            }
        });
    }

    private void gameDownloadEvent(Game game, String buttonText) {
        Platform.runLater(() -> {
            mainScene.getGame_launch_button().setText(buttonText);
            mainScene.getGame_launch_button().setOnAction(event -> downloadGameFiles(game));
        });
    }

    private void gameStopEvent(Process process) {
        Platform.runLater(() -> {
            mainScene.getGame_launch_button().setText("ОСТАНОВИТЬ");
            mainScene.getGame_launch_button().setOnAction(aEvent -> {
                process.children().forEach(processHandle -> {
                    processHandle.destroy();
                    if (processHandle.isAlive()) {
                        processHandle.destroyForcibly();
                    }
                });
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            });
        });
    }

    public void gameStartEvent(Game game) {
        Platform.runLater(() -> {
            mainScene.getGame_launch_button().setText("ИГРАТЬ");
            mainScene.getGame_launch_button().setOnAction(event -> {
                File gameDirectory = new File(folderProperties.getGamesFolder() + game.getName());
                if (gameDirectory.exists()) {
                    long sizeOfDirectory = FileUtils.sizeOfDirectory(gameDirectory);
                    long defaultGameSize = game.getDefaultGameSize();
                    if (defaultGameSize == sizeOfDirectory) {
                        try {
                            game.setProcess(Runtime.getRuntime().exec(
                                    folderProperties.getGamesFolder() +
                                            game.getName() + File.separator +
                                            game.getTitle() + gameProperties.getExeFormat(),
                                    null,
                                    new File(folderProperties.getGamesFolder() + game.getName() + File.separator)));
                            gameStopEvent(game.getProcess());
                            new Thread(() -> {
                                try {
                                    game.getProcess().waitFor();
                                    gameStartEvent(game);
                                } catch (InterruptedException e) {
                                    log.error("Process Interrupted Exception");
                                    gameStartEvent(game);
                                }
                            }).start();
                        } catch (IOException e) {
                            log.error("Ошибка процесса игры {}", game.getTitle());
                        }
                    } else {
                        if (defaultGameSize > sizeOfDirectory && sizeOfDirectory > 0) {
                            gameDownloadEvent(game, "ПРОДОЛЖИТЬ ЗАГРУЗКУ");
                        } else {
                            gameDownloadEvent(game, "УСТАНОВИТЬ");
                        }
                    }
                } else {
                    gameDownloadEvent(game, "УСТАНОВИТЬ");
                }
            });
        });
    }
}
