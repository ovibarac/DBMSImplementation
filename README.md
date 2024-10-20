Run server: `Server`

Run client: `Client`

Test commands:

    create database db;
    use db;
    CREATE TABLE Orders (orderId INT, userId INT, PRIMARY KEY (orderId), FOREIGN KEY (userId) REFERENCES Users(id));
    CREATE TABLE disciplines (DiscID varchar(5) PRIMARY KEY, DName varchar(20), CreditNr int);
    drop table Orders
    DROP DATABASE db;

Result in `db_schema.xml`