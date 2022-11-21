package org.spring.project.application.client.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ResourceProperties {

    @Value("${resource.image.libraryMainImage}")
    private String libraryMainImage;
    @Value("${resource.image.libraryLogo}")
    private String libraryLogo;
    @Value("${resource.image.newsPreviewImage}")
    private String newsPreviewImage;
    @Value("${resource.image.serverErrorImage}")
    private String serverErrorImage;
    @Value("${resource.image.format}")
    private String imageFormat;

}
