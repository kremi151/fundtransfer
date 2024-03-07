package lu.mkremer.fundstransfer.config

import lu.mkremer.fundstransfer.repository.AccountRepository
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackageClasses = [AccountRepository::class])
class PersistenceConfig
