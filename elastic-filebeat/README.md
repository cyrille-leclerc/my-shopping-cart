


### Actual

```
{
   "timestamp":"1615986552269",
   "level":"INFO",
   "thread":"http-nio-8081-exec-1",
   "mdc":{
      "trace_id":"84f205c31249e067a3c400b03b45b739",
      "trace_flags":"01",
      "span_id":"dd0f7192b8ae1375"
   },
   "logger":"org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/]",
   "message":"Initializing Spring DispatcherServlet 'dispatcherServlet'",
   "context":"default"
}
```

### Expected

```
{
   "@timestamp":"2020-07-23T09:27:35.648Z",
   "log.level":"INFO",
   "message":"SUCCESS createOrder([OrderController.OrderForm@6ad576celist[[OrderProductDto@783da093 product = [Product@1eb0be6f id = 6, name = 'Phone', price = 500.0], quantity = 2]]]): totalPrice: 1000.0, id:1850281",
   "service.name":"com-shoppingcart_frontend",
   "event.dataset":"com-shoppingcart_frontend.log",
   "process.thread.name":"http-nio-8080-exec-1",
   "log.logger":"com.mycompany.ecommerce.controller.OrderController",
   "transaction.id":"3cee608983425c61",
   "trace.id":"831a953a92e2e3934aef79444130cc7a"
}
```

