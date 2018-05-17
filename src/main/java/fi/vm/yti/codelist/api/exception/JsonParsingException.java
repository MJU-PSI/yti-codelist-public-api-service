package fi.vm.yti.codelist.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import fi.vm.yti.codelist.common.dto.ErrorModel;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class JsonParsingException extends YtiCodeListException {

    public JsonParsingException(final String errorMessage) {
        super(new ErrorModel(HttpStatus.NOT_ACCEPTABLE.value(), errorMessage));
    }
}
