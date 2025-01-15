Run server: `Server`

Run client: `Client`

Test commands:

    create database db;
    use db;
    CREATE TABLE Orders (orderId INT, userId INT, PRIMARY KEY (orderId), FOREIGN KEY (userId) REFERENCES Users(id));
    CREATE TABLE disciplines (DiscID varchar(5) PRIMARY KEY, DName varchar(20), CreditNr int);
    drop table Orders
    DROP DATABASE db;
    CREATE INDEX idx_orderid ON Orders (orderId)
    CREATE UNIQUE INDEX idx_disc ON disciplines (DiscID, DName)
    INSERT INTO Orders VALUES (19,10);
    INSERT INTO Orders VALUES (3,8);
    INSERT INTO Orders VALUES (1,1);
    DELETE FROM Orders WHERE id = 1;

    INSERT INTO Users VALUES (4,muwu,puwu);
    CREATE TABLE Test (testId INT, uId INT, PRIMARY KEY (testId), FOREIGN KEY (uId) REFERENCES Users(uId));
    CREATE UNIQUE INDEX idx_usrn ON Users (username)
    CREATE INDEX idx_usrn ON Users (username)
    DELETE FROM Users WHERE id = 2;

    INSERT INTO Orders VALUES (3,3);
    INSERT INTO Orders VALUES (3,7);
    INSERT INTO Users VALUES (4,muwu,great);
    INSERT INTO Users VALUES (5,adk,kda);
    INSERT INTO Orders VALUES (3,5);

    SELECT a,b,c FROM Users WHERE a = 7 AND b LIKE @gmail
    SELECT o.orders,u.username FROM Orders o,User u WHERE o.id = 2 AND u.passwd = alala

    SELECT productId from OrderItem WHERE orderId = 1
    SELECT DISTINCT username,passwd from Users WHERE username LIKE abc
    SELECT DISTINCT username,passwd,userId from Users WHERE username LIKE abcd AND passwd LIKE pp AND userId > 8

    CREATE TABLE Products (productId INT, name varchar(20), price INT, color varchar(20), PRIMARY KEY (productId));
    CREATE INDEX idx_product_price ON Products (price)
    1M
    SELECT DISTINCT name,price,color FROM Products WHERE color = Pink
    SELECT DISTINCT name,price,color FROM Products WHERE price < 50
    SELECT DISTINCT productId,name FROM Products WHERE name LIKE a
    SELECT productId,name,price,color FROM Products WHERE productId < 10000

    SELECT u.userId,u.username,o.quantity FROM Users u WHERE u.passwd LIKE a AND o.quantity > 10 AND o.quantity < 15 AND u.username LIKE d JOIN Orders o ON u.userId = o.userId

Result in `db_schema.xml`