# Stock Dojo Backend Server
A Spring Boot API for the Stock Dojo app

## Needed properties files
To connect to DB, add a file `db.properties` in the same directory as the `application.properties` file. Configure:

    spring.datasource.url=jdbc:mysql://<address>:<port>/<schema>?useSSL=false
    spring.datasource.username=<username>
    spring.datasource.password=<password>

To connect to a mail server, add a file `mail.properties` in the same directory as the `application.properties` file. Configure:

    spring.mail.host=smtp.gmail.com
    spring.mail.port=587
    spring.mail.username=<gmail username>
    spring.mail.password=<app password>
    spring.mail.properties.mail.smtp.auth=true
    spring.mail.properties.mail.smtp.starttls.enable=true

Note: An app password is different from the account password

To enable HTTPS, add a file `ssl.properties` in the same directory as the `application.properties` file. Configure:

    server.ssl.key-store-type=PKCS12
    server.ssl.key-store=<path to .p12 file>
    server.ssl.key-alias=<key alias>
    server.ssl.key-store-password=<keystore password>
    server.ssl.enabled=true

## Needed environment variables

    LOG_FILE: path to write the log file to
    FINNHUB_API_KEY: API key for https://finnhub.io/
    FMP_API_KEY: API key for https://site.financialmodelingprep.com/
