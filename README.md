#graylog-plugin-quill-output

* v1.0
    * Only support postgresql now.

* Output Setting Panel Sample:
```
   Title:                   output
   Host:                    localhost
   Port:                    5432
   Database Name:           db_log
   Username:                your_username
   Password:                your_password
   Insert SQL:              insert into tb_log (
                                field1,
                                field2,
                                field3,
                                field4, ...)
                              values (
                                {field1_name_in_log},
                                {field2_name_in_log},
                                {field3_name_in_log},
                                {field4_name_in_log:org.joda.time.DateTime}, ...);

   Bulk Insert Size         default 20
    (optional)
   Timeout                  default 5
    (optional)
   Connection Pool Size     default 0
    (optional)
   Buffer Size              default 0
    (optional)
   
```