```
curl "https://127.0.0.1:8443/Create" --header 'Content-Type: application/json' -X POST --data-binary '{"counter-s89":356109}' --insecure

curl "https://127.0.0.1:8443/Get?counter=counter1"  --insecure

curl "https://127.0.0.1:8443/GetAll"  --insecure

curl -X PATCH "https://127.0.0.1:8443/Increment?counter=counter-7"  --insecure

curl -X DELETE "https://127.0.0.1:8443/Delete?counter=counter-7"  --insecure

```  

[Тесты\документация на API](https://github.com/dzmitrykashlach/counter-service/blob/master/src/test/kotlin/ApplicationTest.kt) 
