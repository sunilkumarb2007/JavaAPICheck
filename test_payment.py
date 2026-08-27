import urllib.request
import json
req = urllib.request.Request('http://localhost:8080/api/payments', data=b'{"amount":100}', headers={'Content-Type':'application/json'}, method='POST')
try:
    print(urllib.request.urlopen(req).read())
except Exception as e:
    print("Error:", e)
