import {sleep, group} from 'k6'
import http from 'k6/http'
import { randomIntBetween, randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    ext: {
        loadimpact: {
            // Test runs with the same name groups test runs together
            name: "MyCompany / Ecommerce Web Site"
        }
    },
    thresholds: {},
    scenarios: {
        Scenario_1: {
            executor: 'ramping-vus',
            gracefulStop: '30s',
            stages: [
                {target: 5, duration: '1m'},
                {target: 5, duration: '30m'},
                {target: 0, duration: '1m'},
            ],
            gracefulRampDown: '30s',
            exec: 'scenario_1',
        },
    },
}

export function scenario_1() {
    let frontendServerRootUrlsAsString

    if (__ENV.FRONTEND_URLS !== undefined) {
        frontendServerRootUrlsAsString = `${__ENV.FRONTEND_URLS}`
    } else {
        frontendServerRootUrlsAsString = "http://localhost:8080"
    }
    const frontendServerRootUrls = frontendServerRootUrlsAsString.split(",")

    let response

    group('Purchase', function () {
        let frontendRootUrl = randomItem(frontendServerRootUrls)

        // Home
        response = http.get(frontendRootUrl)

        // Get Products
        response = http.get(frontendRootUrl + "/api/products", {
            headers: {
                accept: 'application/json, text/plain, */*',
            },
        })

        let product = randomItem(products)
        let quantity = randomIntBetween(1,3)
        let price = product.price * quantity

        let paymentMethod = price > priceUpperBoundaryDollarsOnMediumShoppingCarts ?
            randomItem(unEvenlyDistributedPaymentMethods) :
            randomItem(evenlyDistributedPaymentMethods)
        let shippingMethod = randomItem(shippingMethods)
        let shippingCountry = randomItem(shippingCountries)

        let purchaseOrders = {
            "productOrders": [{
                "product": product,
                "quantity": quantity
            }],
            "paymentMethod": paymentMethod,
            "shippingMethod" : shippingMethod,
            "shippingCountry" : shippingCountry
        }

        sleep(randomIntBetween(1,5))
        // Place Order
        response = http.post(
            frontendRootUrl + "/api/orders",
            JSON.stringify(purchaseOrders),
            {
                headers: {
                    accept: 'application/json, text/plain, */*',
                    'content-type': 'application/json'
                },
            }
        )
        sleep(randomIntBetween(1,5))
        // Get Products
        response = http.get(frontendRootUrl + "/api/products", {
            headers: {
                accept: 'application/json, text/plain, */*'
            },
        })
    })
}

const products = [
    {"id": 1, "name": "TV Set", "price": 300.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 2, "name": "Game Console", "price": 200.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 3, "name": "Sofa", "price": 100.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 4, "name": "Icecream", "price": 5.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 5, "name": "Beer", "price": 3.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 6, "name": "Phone", "price": 500.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 7, "name": "Watch", "price": 30.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 8, "name": "USB Cable", "price": 4.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 9, "name": "USB-C Cable", "price": 5.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 10, "name": "Micro USB Cable", "price": 3.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 11, "name": "Lightning Cable", "price": 9.0, "pictureUrl": "https://placehold.it/200x100"},
    {"id": 12, "name": "USB C adapter", "price": 5.0, "pictureUrl": "https://placehold.it/200x100"},
]

const evenlyDistributedPaymentMethods = [
    "PAYPAL",
    "PAYPAL",
    "PAYPAL",
    "PAYPAL",
    "VISA",
    "VISA",
    "VISA",
    "VISA",
    "AMEX"]
const unEvenlyDistributedPaymentMethods = [
    "AMEX",
    "AMEX",
    "AMEX",
    "AMEX",
    "AMEX",
    "PAYPAL",
    "VISA",
]

const shippingCountries = ["FR", "FR", "GB", "DE"]
const shippingMethods = ["standard", "express"]

const priceUpperBoundaryDollarsOnMediumShoppingCarts = 100