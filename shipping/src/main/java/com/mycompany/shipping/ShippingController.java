package com.mycompany.shipping;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

@RestController()
public class ShippingController {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    final DataSource dataSource;

    List<String> traceContextPropagationHeaderNames = Arrays.asList("traceparent", "tracestate", "uber-trace-id", "X-B3-TraceId", "X-B3-ParentSpanId", "X-B3-SpanId");

    public ShippingController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @RequestMapping(path = "shipOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String shipOrder(HttpServletRequest request) {

        StringBuilder msg = new StringBuilder();
        traceContextPropagationHeaderNames.stream()
                .filter(headerName -> request.getHeader(headerName) != null)
                .map(headerName -> headerName + "=" + request.getHeader(headerName) + " ")
                .forEach(msg::append);
        logger.info("Ship order for {}", msg);

        try (Connection cnn = dataSource.getConnection()) {
            try (Statement stmt = cnn.createStatement()) {
                stmt.execute("select *, pg_sleep_for('1 seconds') from product where id=1");
            }
        } catch (SQLException e) {
            logger.atError().log("Error while shipping order", e);
        }

        return "ok";
    }
}
