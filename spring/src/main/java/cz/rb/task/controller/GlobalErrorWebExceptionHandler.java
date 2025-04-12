package cz.rb.task.controller;

import cz.rb.task.exception.TaskNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Global exception handler for task-related errors.
 * Provides consistent error responses across the API.
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 12.04.2025
 */
@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties.Resources resources,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, resources, applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        log.error("Error handling request: {}", error.getMessage());

        // Map specific exceptions to appropriate HTTP status codes
        HttpStatus status = determineHttpStatus(error);

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of(
                        "error", error.getClass().getSimpleName(),
                        "message", error.getMessage(),
                        "status", status.value(),
                        "path", request.path()
                )));
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof TaskNotFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            return HttpStatus.BAD_REQUEST;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    @Configuration
    public static class ErrorHandlerConfiguration {
        @Bean
        public WebProperties.Resources resources() {
            return new WebProperties.Resources();
        }
    }
}