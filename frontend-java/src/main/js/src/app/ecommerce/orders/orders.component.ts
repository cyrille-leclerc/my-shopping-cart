import {Component, OnInit} from '@angular/core';
import {ProductOrders} from "../models/product-orders.model";
import {Subscription} from "rxjs/internal/Subscription";
import {EcommerceService} from "../services/EcommerceService";

@Component({
    selector: 'app-orders',
    templateUrl: './orders.component.html',
    styleUrls: ['./orders.component.css']
})
export class OrdersComponent implements OnInit {
    orders: ProductOrders;
    total: number;
    paymentStatus: PaymentStatus;
    sub: Subscription;

    constructor(private ecommerceService: EcommerceService) {
        this.orders = this.ecommerceService.ProductOrders;
    }

    ngOnInit() {
        this.paymentStatus = PaymentStatus.NOT_PAID;
        this.sub = this.ecommerceService.OrdersChanged.subscribe(() => {
            this.orders = this.ecommerceService.ProductOrders;
        });
        this.loadTotal();
    }

    pay() {
        this.paymentStatus = PaymentStatus.PAYMENT_FAILURE;
        console.warn("Before invocation saveOrder(): "  + this.orders);
        this.ecommerceService.saveOrder(this.orders).subscribe(resp => {
            console.warn("Response for invocation saveOrder(): "  + resp);
            this.paymentStatus = PaymentStatus.PAYMENT_SUCCESS;
        });
    }

    loadTotal() {
        this.sub = this.ecommerceService.TotalChanged.subscribe(() => {
            this.total = this.ecommerceService.Total;
        });
    }

    isPaymentSuccess() : boolean {
        return this.paymentStatus == PaymentStatus.PAYMENT_SUCCESS;
    }

    isPaymentFailure() : boolean {
        return this.paymentStatus == PaymentStatus.PAYMENT_FAILURE;
    }

    isPaymentNotPaid() : boolean {
        return this.paymentStatus == PaymentStatus.NOT_PAID;
    }

}
export enum PaymentStatus {NOT_PAID, PAYMENT_SUCCESS, PAYMENT_FAILURE}

