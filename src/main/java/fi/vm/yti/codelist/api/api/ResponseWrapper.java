package fi.vm.yti.codelist.api.api;

import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import fi.vm.yti.codelist.common.dto.Meta;

@XmlRootElement
@XmlType(propOrder = { "meta", "results" })
public class ResponseWrapper<T> {

    private Meta meta;

    private Set<T> results;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(final Meta meta) {
        this.meta = meta;
    }

    public Set<T> getResults() {
        return results;
    }

    public void setResults(final Set<T> results) {
        this.results = results;
    }
}
