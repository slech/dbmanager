
dbmanager
=========
dbmanager provides a fluent API to write test data needed for unit tests to a database. 

I have used [dbunit](http://dbunit.sourceforge.net) and [dbsetup](http://dbsetup.ninja-squad.com) for serval years, but
both did not satisfy me. I preferred to create the test data in code as it is done in *dbsetup*, but the test data was hard 
to maintain because of misspelled column or table names or wrong used datatypes.

Therefore, I decided to write my own API. The drawback of this API is that you have to write an interface for each database 
table describing that table. On the other hand, you do not have to create constants for column or table names. In addition, 
it is easy to create the interface if you have already a JPA entity, as we will see later.
Let us start with an example. 

Example table
-------------
We have the following table:
```SQL
create table user (
  id int(11) not null,
  lastname varchar(255) not null,
  firstname varchar(255) not null,
  login varchar(255) not null,
  password varchar(255),
  usertype varchar(255),
  active int(1) not null,
  primary key (id)
);
```

Creating the interface
----------------------
In order to describe this table we have to create the following interface
```Java
import de.slech.dbmanager.TableManager;

public interface User extends TableManager<User> {
    User id(int id);
    User lastname(String lastname);
    User firstname(String firstname);
    User login(String login);
    User password(String password);
    User usertype(String usertype);
    User active(boolean active);
}
```
The interface must extend *TableManager* with itself as type parameter. The name of the interface has to be the table name, and the names of the methods have to be the column names. All methods must return the interface itself.

You can also use annotations to define table or column names:
```Java
import de.slech.dbmanager.TableManager;
import javax.persistence.Table;
import javax.persistence.Column;

@Table(name = "user")
public interface IUser extends TableManager<IUser> {
    IUser id(int id);
    @Column(name = "lastname")
    IUser lname(String lname);
    @Column(name = "firstname")
    IUser fname(String fname);
    IUser login(String login);
    IUser password(String password);
    IUser usertype(String usertype);
    IUser active(boolean active);
}
```
Now let us assume that we have an enum for *usertype*:
```Java
public enum UserType {
    RESTRICED_USER,
    USER,
    ADMIN
}
```
We can use a converter in order to use the enum in our interface:
```Java
import de.slech.dbmanager.TableManager;
import de.slech.dbmanager.EnumToNameConverter;
import javax.persistence.Convert

public interface User extends TableManager<User> {
    User id(int id);
    User lastname(String lastname);
    User firstname(String firstname);
    User login(String login);
    User password(String password);
    @Convert(converter = EnumToNameConverter.class)
    User usertype(UserType usertype);
    User active(boolean active);
}
```
Writing data into the table
---------------------------
Now we are going to write some test data into our table *user*.
```Java
final DataSource dataSource = createDatasource(); // a datasource is needed 
final DatabaseManager dbm = new DatabaseManager(dataSource);
dbm.createTableManager(IUser.class)
        .generateValuesFor().id(1)
        .setDefaultValues().active(true).usertype("admin")
        .newInsertStatementWithRow()
            .fname("Fritz").lname("Huber").login("fritzhuber").password("****")
        .andRow()
            .fname("Franz").lname("Meier").login("franzmeier")
        .executeStatement();
```
The line `generateValuesFor().id(1)` achieves that the values for the column id are a sequence of numbers starting at 1.
And the line `setDefaultValues().active(true).usertype("admin")` causes that in all rows the values of the columns *active* and 
usertype are *true* and *admin*, respectively.
With `newInsertStatementWithRow()` we start our first row, `andRow()` starts the second row and `executeStatement()` executes the 
insert statement with the two rows.
This code creates the following content of table *user*:

id |lastname|firstname|login|password|usertype|active
---|--------|---------|-----|--------|--------|------
1  |Huber		|Fritz    |fritzhuber|****|admin  |1
2  |Meier   |Franz    |franzmeier|NULL|admin  |1
