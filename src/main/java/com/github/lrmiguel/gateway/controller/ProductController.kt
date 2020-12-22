package com.github.lrmiguel.gateway.controller

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import com.github.lrmiguel.entity.Product
import com.github.lrmiguel.gateway.repository.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@RestController
class ProductController {

    @Autowired
    lateinit var productRepository: ProductRepository

    @Autowired
    lateinit var webClient: WebClient

    @GetMapping("/{id}")
    suspend fun findOne(id: Int): Product? {
        return productRepository.getProductById(id)
    }

    @FlowPreview
    @GetMapping("/")
    fun findAll(): Flow<Product> {
        return productRepository.getAllProducts()
    }

    @GetMapping("/{id}/stock")
    suspend fun findOneInStock(@PathVariable id: Int): ProductStockView = coroutineScope {
        val product: Deferred<Product?> = async(start = CoroutineStart.LAZY) {
            productRepository.getProductById(id)
        }
        val stockQuantity: Deferred<Int> = async(start = CoroutineStart.LAZY) {
            webClient.get()
                    .uri("/stock-service/product/$id/quantity")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .awaitBody<Int>()
        }
        ProductStockView(product.await()!!, stockQuantity.await())
    }
}