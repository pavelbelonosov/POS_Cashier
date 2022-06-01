var url = contextRoot + "api/v1/transactions";
var http = new XMLHttpRequest();

function makePayment() {
    var transaction = {
        amount: document.getElementById("amount").value
    };
    http.open("POST", url+"/pay", true);
    http.setRequestHeader('Content-Type', 'application/json');
    http.send(JSON.stringify(transaction));
}