package io.github.yuanbug.drawer.test.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;

import java.io.IOException;

/**
 * @author yuanbug
 */
public class GenericWithMethodTemplatePattern {

    public interface Service {

        void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException;

        default String getName() {
            return this.getClass().getSimpleName();
        }

    }

    public static abstract class AbstractService<T extends Content, R> implements Service {

        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
            writeResponse(response, doHandle(parseContent(request)));
        }

        protected abstract R doHandle(T content);

        protected abstract Class<T> getContentType();

        private T parseContent(HttpServletRequest request) throws IOException {
            return objectMapper.readValue(request.getInputStream(), getContentType());
        }

        private void writeResponse(HttpServletResponse response, R result) throws IOException {
            response.getWriter().println(objectMapper.writeValueAsString(result));
        }

    }

    public static class LoginService extends AbstractService<LoginForm, Boolean> {
        @Override
        protected Boolean doHandle(LoginForm content) {
            if (!"admin".equals(content.getUsername())) {
                return false;
            }
            return "123123".equals(content.getPassword());
        }

        @Override
        protected Class<LoginForm> getContentType() {
            return LoginForm.class;
        }

        @Override
        public String getName() {
            return "FuckingLogin";
        }

    }

    public interface Content {}

    @Data
    public static class LoginForm implements Content {
        private String username;
        private String password;
    }

}
