// --- Import Section ---
import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;
import java.util.Properties;

// --- Account.java ---
@Entity
@Table(name = "accounts")
class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String owner;
    private double balance;

    public Account() {}
    public Account(String owner, double balance) {
        this.owner = owner;
        this.balance = balance;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return "Account{id=" + id + ", owner='" + owner + "', balance=" + balance + "}";
    }
}

// --- TransactionRecord.java ---
@Entity
@Table(name = "transactions")
class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int fromAccountId;
    private int toAccountId;
    private double amount;
    private Date date;

    public TransactionRecord() {}
    public TransactionRecord(int from, int to, double amount) {
        this.fromAccountId = from;
        this.toAccountId = to;
        this.amount = amount;
        this.date = new Date();
    }

    // Getters and setters
    public int getId() { return id; }
    public int getFromAccountId() { return fromAccountId; }
    public int getToAccountId() { return toAccountId; }
    public double getAmount() { return amount; }
    public Date getDate() { return date; }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", from=" + fromAccountId + ", to=" + toAccountId + ", amount=" + amount + ", date=" + date + "}";
    }
}

// --- Hibernate Configuration (AppConfig.java) ---
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = "com.example")
class AppConfig {

    @Bean
    public LocalSessionFactoryBean sessionFactory() {
        LocalSessionFactoryBean sf = new LocalSessionFactoryBean();
        sf.setPackagesToScan("com.example");
        sf.setHibernateProperties(hibernateProperties());
        sf.setAnnotatedClasses(Account.class, TransactionRecord.class);
        return sf;
    }

    private Properties hibernateProperties() {
        Properties props = new Properties();
        props.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        props.setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/your_database");
        props.setProperty("hibernate.connection.username", "your_username");
        props.setProperty("hibernate.connection.password", "your_password");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        props.setProperty("hibernate.show_sql", "true");
        return props;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}

// --- AccountRepository.java ---
@Repository
class AccountRepository {
    @Autowired
    private SessionFactory sessionFactory;

    public Account getAccount(int id) {
        return sessionFactory.getCurrentSession().get(Account.class, id);
    }

    public void updateAccount(Account account) {
        sessionFactory.getCurrentSession().update(account);
    }

    public void saveTransaction(TransactionRecord tx) {
        sessionFactory.getCurrentSession().save(tx);
    }

    public void saveAccount(Account account) {
        sessionFactory.getCurrentSession().save(account);
    }

    public List<Account> getAllAccounts() {
        return sessionFactory.getCurrentSession().createQuery("from Account", Account.class).list();
    }
}

// --- BankService.java ---
@Service
class BankService {
    @Autowired
    private AccountRepository repo;

    @Transactional
    public void transferMoney(int fromId, int toId, double amount) {
        Account from = repo.getAccount(fromId);
        Account to = repo.getAccount(toId);

        if (from == null || to == null) throw new RuntimeException("Invalid account(s)");
        if (from.getBalance() < amount) throw new RuntimeException("Insufficient funds");

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        repo.updateAccount(from);
        repo.updateAccount(to);

        repo.saveTransaction(new TransactionRecord(fromId, toId, amount));

        System.out.println("Transfer successful: " + amount);
    }

    @Transactional
    public void createAccounts() {
        repo.saveAccount(new Account("Alice", 1000));
        repo.saveAccount(new Account("Bob", 500));
        System.out.println("Accounts created.");
    }

    @Transactional(readOnly = true)
    public void printAccounts() {
        List<Account> list = repo.getAllAccounts();
        list.forEach(System.out::println);
    }
}

// --- MainApp.java ---
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class MainApp {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        BankService service = context.getBean(BankService.class);

        try {
            service.createAccounts();
            service.printAccounts();

            // Success case
            service.transferMoney(1, 2, 200);
            service.printAccounts();

            // Failure case (insufficient funds)
            service.transferMoney(2, 1, 1000); // This will fail and rollback
        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }

        service.printAccounts();
        context.close();
    }
}

