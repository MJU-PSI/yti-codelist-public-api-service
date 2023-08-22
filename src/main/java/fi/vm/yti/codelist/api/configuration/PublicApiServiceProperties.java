package fi.vm.yti.codelist.api.configuration;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("application")
@Component
@Validated
public class PublicApiServiceProperties {

    @NotNull
    private String publicUrl;

    @NotNull
    private String contextPath;

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(final String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

}
