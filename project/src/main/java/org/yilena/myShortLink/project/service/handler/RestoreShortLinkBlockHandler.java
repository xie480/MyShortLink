package org.yilena.myShortLink.project.service.handler;

import org.springframework.stereotype.Service;
import org.yilena.myShortLink.project.common.convention.errorCode.codes.SystemErrorCodes;
import org.yilena.myShortLink.project.common.convention.exception.SystemException;
import org.yilena.myShortLink.project.common.convention.result.Result;
import org.yilena.myShortLink.project.common.convention.result.Results;

@Service
public class RestoreShortLinkBlockHandler {

    public static Result<Void> handle(String shortUri) {
        /*
            直接熔断即可
         */
        return Results.failure(new SystemException(SystemErrorCodes.SYSTEM_ERROR));
    }
}
