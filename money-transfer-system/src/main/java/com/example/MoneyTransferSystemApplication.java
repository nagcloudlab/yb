package com.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Entity
@Table(name = "accounts")
@Getter
@Setter
class Account{
	@Id
	private UUID account_id;
	private double balance;
}


interface AccountRepository extends JpaRepository<Account, UUID> {
}

@Service
class UPITransferService{


	@Autowired
	private AccountRepository accountRepository;

	private CassandraTemplate cassandraTemplate;

	@Transactional(
			isolation = Isolation.READ_COMMITTED,
			timeout = 15
	)
	public void transferMoney(UUID from, UUID to, double amount){

		// YSQL
		Account fromAccount = accountRepository.findById(from).get();
		Account toAccount = accountRepository.findById(to).get();
		fromAccount.setBalance(fromAccount.getBalance() - amount);
		toAccount.setBalance(toAccount.getBalance() + amount);
		accountRepository.save(fromAccount);
		accountRepository.save(toAccount);

		// transaction_history


	}
}


@SpringBootApplication
public class MoneyTransferSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyTransferSystemApplication.class, args);
	}


	@Bean
	public CommandLineRunner demo(UPITransferService upiTransferService) {
		return (args) -> {
			upiTransferService.transferMoney(UUID.fromString("47ca555e-3cfd-4173-afc5-c3f1f5ec603e"), UUID.fromString("23108f2f-c16e-425c-b11e-1d6ef1cf6907"), 100);
		};
	}

}
