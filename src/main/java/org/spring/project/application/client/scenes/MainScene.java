package org.spring.project.application.client.scenes;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import lombok.Getter;
import org.spring.project.application.client.elements.LibraryListElement;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Getter
@Component
public class MainScene {
    //    Top interface - start
    private final MenuItem logout_menu_item = new MenuItem("Войти в другой аккаунт");

    private final Menu stream_button = new Menu("Stream");
    private final Menu view_button = new Menu("Вид");
    private final Menu friends_button = new Menu("Друзья");
    private final Menu games_button = new Menu("Игры");
    private final Menu help_button = new Menu("Справка");
    private final MenuBar stream_menu_MenuBar = new MenuBar(
            stream_button, view_button, friends_button, games_button, help_button);

    private final Pane stream_and_close_menu_separator_pane = new Pane();

    private final Button collapse_button = new Button("_");
    private final Button expand_button = new Button("[ ]");
    private final Button close_button = new Button("X");
    private final HBox close_menu_hBox = new HBox(collapse_button, expand_button, close_button);

    private final HBox stream_and_close_menu_hBox = new HBox(
            stream_menu_MenuBar,
            stream_and_close_menu_separator_pane,
            close_menu_hBox);

    private final Button backward_button = new Button("<");
    private final Button forward_button = new Button(">");
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final ToggleButton store_toggleButton = new ToggleButton("МАГАЗИН");
    private final ToggleButton library_toggleButton = new ToggleButton("БИБЛИОТЕКА");
    private final ToggleButton community_toggleButton = new ToggleButton("СООБЩЕСТВО");
    private final ToggleButton profile_toggleButton = new ToggleButton("ПРОФИЛЬ");

    private final HBox store_library_menu_hBox = new HBox(
            backward_button, forward_button, store_toggleButton,
            library_toggleButton, community_toggleButton, profile_toggleButton);

    private final VBox main_top_vBox = new VBox(stream_and_close_menu_hBox, store_library_menu_hBox);

    private final TextField search_textField = new TextField();
    private final ListView<LibraryListElement> games_list_listView = new ListView<>();
    private final VBox main_library_left_vBox = new VBox(search_textField, games_list_listView);
    //    Top interface - end

    //    Library main page - start
    private final FlowPane library_main_page_flowPane = new FlowPane();
    //    Library main page - end

    //    Game news - start
    private final ImageView game_preview_image_imageView = new ImageView();

    private final Button game_launch_button = new Button();
    private final HBox game_launch_menu_hBox = new HBox();

    private final VBox update_news_vBox = new VBox();
    private final VBox achievements_vBox = new VBox();
    private final HBox update_news_and_achievements_menu_hBox = new HBox(update_news_vBox, achievements_vBox);

    private final VBox library_center_menu_vBox = new VBox(
            game_preview_image_imageView,
            game_launch_menu_hBox,
            update_news_and_achievements_menu_hBox);
    private final ScrollPane main_library_center_scrollPane = new ScrollPane(library_center_menu_vBox);
    private final AnchorPane main_library_center_anchorPane = new AnchorPane(library_main_page_flowPane);
    //    Game news - end

    //    Html interface - start
    private WebView webView;
    private WebEngine webEngine;
    //    Html interface - end

    private final BorderPane main_borderPane = new BorderPane();

    @PostConstruct
    protected void init() {

//        Top interface - start
        stream_button.getItems().add(logout_menu_item);
        stream_menu_MenuBar.setId("stream_menu_MenuBar");
        HBox.setHgrow(stream_and_close_menu_separator_pane, Priority.ALWAYS);
        stream_and_close_menu_hBox.setId("stream_and_close_menu_hBox");

        store_toggleButton.setToggleGroup(toggleGroup);
        library_toggleButton.setToggleGroup(toggleGroup);
        community_toggleButton.setToggleGroup(toggleGroup);
        profile_toggleButton.setToggleGroup(toggleGroup);
        toggleGroup.selectToggle(library_toggleButton);
        store_library_menu_hBox.setId("store_library_menu_hBox");

        main_top_vBox.getStylesheets().add("/css/homepage/main_top_vBox_style.css");
//        Top interface - end

//        Library left interface - start
        search_textField.setId("search_textField");
        VBox.setVgrow(games_list_listView, Priority.ALWAYS);
        main_library_left_vBox.setId("main_library_left_vBox");
        main_library_left_vBox.getStylesheets().add("/css/homepage/main_library_left_vBox_style.css");
//        Library left interface - end

//        Library main page - start
        library_main_page_flowPane.setId("library_main_page_flowPane");
        library_main_page_flowPane.setHgap(20);
        library_main_page_flowPane.setVgap(20);
        AnchorPane.setTopAnchor(library_main_page_flowPane, 0.0);
        AnchorPane.setRightAnchor(library_main_page_flowPane, 0.0);
        AnchorPane.setBottomAnchor(library_main_page_flowPane, 0.0);
        AnchorPane.setLeftAnchor(library_main_page_flowPane, 0.0);
        AnchorPane.setTopAnchor(main_library_center_scrollPane, 0.0);
        AnchorPane.setRightAnchor(main_library_center_scrollPane, 0.0);
        AnchorPane.setBottomAnchor(main_library_center_scrollPane, 0.0);
        AnchorPane.setLeftAnchor(main_library_center_scrollPane, 0.0);
//        Library main page - end

//        Game news - start
        game_preview_image_imageView.fitWidthProperty().bind(main_library_center_scrollPane.widthProperty());
        game_preview_image_imageView.setPreserveRatio(true);

        game_launch_menu_hBox.setId("game_launch_menu_hBox");
        game_launch_menu_hBox.setStyle(" -fx-background-color: rgba(0, 0, 0, 0.2)");

        update_news_and_achievements_menu_hBox.setId("update_news_and_achievements_menu_hBox");
        update_news_and_achievements_menu_hBox.setStyle(" -fx-background-color: rgba(0, 0, 0, 0.2)");
        update_news_vBox.setId("update_news_vBox");
        HBox.setHgrow(update_news_vBox, Priority.ALWAYS);
        update_news_vBox.setPadding(new Insets(0, 30, 30, 30));
        achievements_vBox.setId("achievements_vBox");
        HBox.setHgrow(achievements_vBox, Priority.ALWAYS);
        update_news_and_achievements_menu_hBox.setId("update_news_and_achievements_menu_hBox");
        VBox.setVgrow(update_news_and_achievements_menu_hBox, Priority.ALWAYS);

        library_center_menu_vBox.setId("library_center_menu_vBox");

        main_library_center_scrollPane.setFitToWidth(true);
        main_library_center_scrollPane.setFitToHeight(true);
        main_library_center_scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        main_library_center_scrollPane.setId("main_library_center_scrollPane");
        main_library_center_scrollPane.getStylesheets().add("/css/homepage/main_library_center_scrollPane_style.css");
//        Game news - end

        main_borderPane.setId("main_borderPane");
        main_borderPane.getStylesheets().add("/css/homepage/main_border_pane_style.css");
//        Scene element events - end

        Platform.runLater(() -> {
            webView = new WebView();
            webEngine = webView.getEngine();
        });
    }
}