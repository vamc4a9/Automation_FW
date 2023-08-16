package com.qa.core.api;

import io.qameta.allure.Allure;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

import static io.restassured.filter.log.LogDetail.ALL;

/**
 * RestAssured response filter to enable rest response logging to allure report.
 */
public class RestResponseAllureFilter implements Filter {

    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        Response response = ctx.next(requestSpec, responseSpec);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (PrintStream printStream = new PrintStream(outputStream)) {
                StringBuilder sb = new StringBuilder();

                ResponsePrinter.print(response, response, printStream, ALL, true, Collections.emptySet());
                final byte[] responseBody = response.asByteArray();

                sb.append(outputStream);
                if (responseBody != null) {
                    sb.append(System.getProperty("line.separator"));
                    sb.append(new String(responseBody));
                }

                Allure.addAttachment("response", sb.toString());
                response = cloneResponseIfNeeded(response, responseBody);

                return response;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;

    }

    private Response cloneResponseIfNeeded(Response response, byte[] responseAsString) {
        if (responseAsString != null && response instanceof RestAssuredResponseImpl &&
            !((RestAssuredResponseImpl) response).getHasExpectations()) {
            final Response build = new ResponseBuilder().clone(response).setBody(responseAsString).build();
            ((RestAssuredResponseImpl) build).setHasExpectations(true);
            return build;
        }
        return response;
    }

}
