var url = contextRoot + "api/v1/transactions";
var http = new XMLHttpRequest();

function makePayment() {

  /*var price = prompt("Ведите сумму оплаты и следуйте указаниям на пинпаде", "0.0");
  if (price == null || price == ""|| price < 1){
    text = "Неккоректная сумма! ";
    document.getElementById("transactionInfo").innerHTML = text;
    return;
}*/
 var products = document.getElementsByName('prodName');
 var productsAmount = document.getElementsByName('prodAmount');
 //const map = new Map();
 if(products.length==productsAmount.length){
 var prodList=[];
 products.forEach(p=>prodList.push(p.textContent));
 var prodAmountList=[];
 productsAmount.forEach(a=>prodAmountList.push(a.textContent));
};
 /*if(products.length==productsAmount.length){
  for(var i=0, n=products.length;i<n;i++) {
    map.set(products[i].textContent,productsAmount[i].textContent);
    }
 }*/
  var transaction = {
          amount: document.getElementById("totalPrice").textContent,
          productsList: prodList,
          productsAmountList: prodAmountList
      };

console.log(transaction);

    http.open("POST", url+"/pay", true);
    http.setRequestHeader('Content-Type', 'application/json');
    http.send(JSON.stringify(transaction));
    //setTimeout(reloadPage,5000);

    http.onreadystatechange = function () {
    if (this.readyState != 4) {
        return;
    }
    var transactionResponsed = JSON.parse(this.responseText)
    if(transactionResponsed.status==false){
    document.getElementById("paymentBtn").innerHTML = "Отказано";
     document.getElementById("paymentBtn").className = "btn btn-danger";
    } else{
    document.getElementById("paymentBtn").innerHTML = "Одобрено" ;
    document.getElementById("paymentBtn").className = "btn btn-success";

    }

        document.getElementById("totalPrice").textContent="";
        var productsInCart = document.getElementsByName('productInCart');
        while(productsInCart.length>0){
        productsInCart.forEach(e=>e.remove());
        }
        for(var i=0, n=products.length;i<n;i++) {
            products[i].textContent="";
            productsAmount[i].textContent="";
            }
            document.getElementById("jumbotronChequeArea").className = "jumbotron";
            document.getElementById("responseCheque").innerHTML = transactionResponsed.cheque;

    }


}

function reloadPage(){
window.location.reload();
}
