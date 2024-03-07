package lu.mkremer.fundstransfer.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class WebMvcConfig {

    @Bean
    fun restTemplate(): RestTemplate = RestTemplateBuilder().build()

}
