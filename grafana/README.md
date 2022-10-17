
```
sum by(paymentMethod) (rate({filename="/usr/local/var/log/my-shopping-cart/frontend.log"} |= `SUCCESS placeOrder` | logfmt | unwrap price [5m]))
```