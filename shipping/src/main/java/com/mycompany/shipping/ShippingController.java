package com.mycompany.shipping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController()
public class ShippingController {
    final static Random RANDOM = new Random();

    final DataSource dataSource;

    public ShippingController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @RequestMapping(path = "shipOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String shipOrder(HttpServletRequest request) {
        List<String> headerNames = Arrays.asList("traceparent", "tracestate", "uber-trace-id", "X-B3-TraceId", "X-B3-ParentSpanId", "X-B3-SpanId");

        String msg = "";
        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                msg += " " + headerName + "=" + headerValue;
            }
        }
        System.out.println(msg);

        try (Connection cnn = dataSource.getConnection()) {
            try (Statement stmt = cnn.createStatement()) {
                stmt.execute("select pg_sleep(0.05)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "ok";
    }
}
