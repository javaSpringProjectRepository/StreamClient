package org.spring.project.application.client;

import org.spring.project.application.client.application.FxApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static javafx.application.Application.launch;

@SpringBootApplication
public class SpringClientApplicationClient {

	public static void main(String[] args) {
		launch(FxApplication.class);
	}
}
