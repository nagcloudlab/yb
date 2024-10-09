package com.example;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
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
//	@Query("SELECT a FROM Account a WHERE a.account_id = ?1")
//	public Account loadAccount(UUID account_id);
}

@Service
@RequiredArgsConstructor
class TransferService{
	private final AccountRepository accountRepository;

	@Transactional(
			transactionManager = "transactionManager",
			isolation = Isolation.READ_COMMITTED,
			propagation = Propagation.REQUIRED,
			timeout = 15,
			rollbackFor = RuntimeException.class
	)
	public  void transfer(UUID from, UUID to, double amount){

		Account fromAccount = accountRepository.findById(from).get();
		Account toAccount = accountRepository.findById(to).get();

		fromAccount.setBalance(fromAccount.getBalance() - amount);

		// Simulate a exception
		boolean flag = true;
		if(flag){
			throw new RuntimeException("Simulate a exception");
		}

		toAccount.setBalance(toAccount.getBalance() + amount);

		accountRepository.save(fromAccount);
		accountRepository.save(toAccount);
	}
}


@SpringBootApplication
public class MoneyTransferServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyTransferServiceApplication.class, args);
	}


	@Bean
	public CommandLineRunner demo(AccountRepository accountRepository, TransferService transferService) {
		return (args) -> {
			transferService.transfer(UUID.fromString("47ca555e-3cfd-4173-afc5-c3f1f5ec603e"), UUID.fromString("23108f2f-c16e-425c-b11e-1d6ef1cf6907"), 100);
		};
	}

}
