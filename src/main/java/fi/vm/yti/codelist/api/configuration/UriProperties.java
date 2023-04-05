package fi.vm.yti.codelist.api.configuration;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("uri")
@Component
@Validated
public class UriProperties {

    @NotNull
    private String scheme;

    @NotNull
    private String host;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public String getUriHostAddress(){
        return this.scheme + "://" + this.host;
    }
}
