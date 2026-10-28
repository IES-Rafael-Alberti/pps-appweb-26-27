package com.vetclinic.sale;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetclinic.ApiIntegrationTestBase;
import com.vetclinic.food.FoodProduct;
import com.vetclinic.owner.Owner;
import com.vetclinic.sale.dto.CreateSaleRequest;
import com.vetclinic.sale.dto.CreateSaleRequest.SaleLineRequest;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Proves the sale flow cannot oversell: with a single unit left, two cash registers tapping at the same
 * moment must yield exactly one 201 and one 409. Exercises the PESSIMISTIC_WRITE lock end to end.
 */
@DisplayName("Concurrent sales")
class ConcurrentSaleTest extends ApiIntegrationTestBase {

    @Test
    @DisplayName("only one of two simultaneous sales of the last unit succeeds")
    void onlyOneSaleWinsTheLastUnit() throws Exception {
        FoodProduct lastBag = givenProduct("Last bag", "10.00", 1);
        Owner lucia = givenOwner("Lucia", "lucia@example.com");

        CreateSaleRequest sale = new CreateSaleRequest(lucia.getId(), "Lucia", PaymentMethod.CASH, null,
                List.of(new SaleLineRequest(lastBag.getId(), 1)), null);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);

            Callable<String> register = () -> {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
                try {
                    return foodSaleService.registerSale(sale).saleNumber();
                } catch (RuntimeException ex) {
                    return ex.getClass().getSimpleName() + ": " + ex.getMessage();
                }
            };

            Future<String> first = pool.submit(register);
            Future<String> second = pool.submit(register);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            String firstResult = first.get(10, TimeUnit.SECONDS);
            String secondResult = second.get(10, TimeUnit.SECONDS);

            List<String> results = List.of(firstResult, secondResult);
            long succeeded = results.stream().filter(r -> r.startsWith("VET-")).count();

            assertThat(succeeded)
                    .as("exactly one ticket must be created, got %s", results)
                    .isEqualTo(1);
            assertThat(foodProductRepository.findById(lastBag.getId()).orElseThrow().getStock()).isZero();
            assertThat(foodSaleRepository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
