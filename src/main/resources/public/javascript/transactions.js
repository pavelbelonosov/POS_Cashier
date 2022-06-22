var url = contextRoot + "api/v1/transactions";
var http = new XMLHttpRequest();
var text;

function makePayment() {

  var price = prompt("Ведите сумму оплаты и следуйте указаниям на пинпаде", "0.0");
  if (price == null || price == ""|| price < 1){
    text = "Неккоректная сумма! ";
    document.getElementById("transactionInfo").innerHTML = text;
    return;
}
    var transaction = {
        amount: price
    };

    http.open("POST", url+"/pay", true);
    http.setRequestHeader('Content-Type', 'application/json');
    http.send(JSON.stringify(transaction));
    setTimeout(reloadPage,5000);

}

function reloadPage(){
window.location.reload();
}
