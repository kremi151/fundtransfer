package lu.mkremer.fundstransfer.config

import lu.mkremer.fundstransfer.repository.AccountRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EnableJpaRepositories(basePackageClasses = [AccountRepository::class])
class PersistenceConfig
