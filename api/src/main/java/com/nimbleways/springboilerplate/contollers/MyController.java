package com.nimbleways.springboilerplate.contollers;

import com.nimbleways.springboilerplate.dto.product.ProcessOrderResponse;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.ProductService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class MyController {
    @Autowired
    private ProductService ps;

    @Autowired
    private ProductRepository pr;

    @Autowired
    private OrderRepository or;

    @PostMapping("{orderId}/processOrder")
    @ResponseStatus(HttpStatus.OK)
    public ProcessOrderResponse processOrder(@PathVariable Long orderId) {
        Order order = or.findById(orderId).get();
        System.out.println(order);
        List<Long> ids = new ArrayList<>();
        ids.add(orderId);
        Set<Product> products = order.getItems();
        products.stream().forEach(p -> {
            switch (p.getType()){
                case "NORMAL":
                    if (p.getAvailable() > 0) {
                        buyOneProduct(p);
                    } else {
                        int leadTime = p.getLeadTime();
                        if (leadTime > 0) {
                            ps.notifyDelay(leadTime, p);
                        }
                    }
                    break;

                    case "SEASONAL":
                        if ((LocalDate.now().isAfter(p.getSeasonStartDate()) && LocalDate.now().isBefore(p.getSeasonEndDate())
                                && p.getAvailable() > 0)) {
                            buyOneProduct(p);
                        } else {
                            ps.handleSeasonalProduct(p);
                        }
                    break;

                    case "EXPIRABLE":
                        if (p.getAvailable() > 0 && p.getExpiryDate().isAfter(LocalDate.now())) {
                            buyOneProduct(p);
                        } else {
                            ps.handleExpiredProduct(p);
                        }
                    break;

                case "FLASHSALE":
                        if(p.getAvailable() > p.getMaxFlashsaleValue() && LocalDate.now().isAfter(p.getFlashsaleSeasonStartDate()) && LocalDate.now().isBefore(p.getFlashsaleSeasonEndDate())){
                            buyOneProduct(p);
                        }else {
                            ps.handleFlashsaleProduct(p);
                        }
                    break;
            }
        });

        return new ProcessOrderResponse(order.getId());
    }

    private void buyOneProduct(Product p){
        p.setAvailable(p.getAvailable() - 1);
        pr.save(p);
    }
}
