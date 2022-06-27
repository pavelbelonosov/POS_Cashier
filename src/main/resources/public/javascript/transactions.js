var url = contextRoot + "api/v1/transactions";
var http = new XMLHttpRequest();

function makePayment() {
http.open("POST", url+"/pay", true);
http.setRequestHeader('Content-Type', 'application/json');
http.send(JSON.stringify(createTransactionObject()));
getResponseFromServerWithCheque("paymentBtn");
}

function makeRefund(){
alert("Вы уверены, что хотите сделать возврат?");
http.open("POST", url+"/refund", true);
http.setRequestHeader('Content-Type', 'application/json');
http.send(JSON.stringify(createTransactionObject()));
getResponseFromServerWithCheque("refundBtn");
}

function createTransactionObject(){
var products = document.getElementsByName('prodName');
var productsAmount = document.getElementsByName('prodAmount');

if(products.length==productsAmount.length){
var prodList=[];
products.forEach(p=>prodList.push(p.textContent));
var prodAmountList=[];
productsAmount.forEach(a=>prodAmountList.push(a.textContent));
};

var transaction = {
          amount: document.getElementById("totalPrice").textContent,
          productsList: prodList,
          productsAmountList: prodAmountList,
      };

console.log(transaction);
return transaction;
}

function getResponseFromServerWithCheque(buttonId){
 http.onreadystatechange = function () {
    if (this.readyState != 4) {
        return;
    }
    var transactionResponsed = JSON.parse(this.responseText)
    if(transactionResponsed.status==false){
    document.getElementById(buttonId).innerHTML = "Отказано";
     document.getElementById(buttonId).className = "btn btn-danger";
    } else {
    document.getElementById(buttonId).innerHTML = "Одобрено" ;
    document.getElementById(buttonId).className = "btn btn-success";

    }

    clearProductsCart();
    document.getElementById("jumbotronChequeArea").className = "jumbotron";
    document.getElementById("responseCheque").innerHTML = transactionResponsed.cheque;

    }
}

function clearProductsCart(){
   /* for(var i=0, n=products.length;i<n;i++) {
            products[i].textContent="";
            productsAmount[i].textContent="";
            }*/
var productsInCart = document.getElementsByName('productInCart');
        while(productsInCart.length>0){
        productsInCart.forEach(e=>e.remove());
        }
document.getElementById("totalPrice").textContent="";
}

function reloadPage(){
window.location.reload();
}
