var url = contextRoot + "api/v1/transactions";
var http = new XMLHttpRequest();


function makePayment() {
    if(!verifyCartEmpty()){
        http.open("POST", url+"/pay", true);
        http.setRequestHeader('Content-Type', 'application/json');
        http.send(JSON.stringify(createTransactionObject()));
        getResponseFromServerWithCheque("paymentBtn");
    }
}

function makeRefund(){
    if(!verifyCartEmpty()){
        if (!confirm("Вы уверены, что хотите сделать возврат?")) {
            return;
        }
        http.open("POST", url+"/refund", true);
        http.setRequestHeader('Content-Type', 'application/json');
        http.send(JSON.stringify(createTransactionObject()));
        getResponseFromServerWithCheque("refundBtn");
    }
}

function closeDay(){
    if (!confirm("Вы уверены, что хотите закрыть смену?")) {
        return;
    }
    http.open("GET", url+"/closeday", true);
    http.send();
    getResponseFromServerWithCheque("zReportBtn");
}

function makeXreport(){
    http.open("GET", url+"/xreport", true);
    http.send();
    getResponseFromServerWithCheque("xReportBtn");
}

function getTransactionsStat() {
    http.open("GET", url+"/stat", true);
    http.send();
    http.onreadystatechange = function () {
        if (this.readyState != 4 || this.status != 200) {
            return
        }
        var stat = JSON.parse(this.responseText);
        console.log(stat);
        document.getElementById("currentBalance").innerHTML = stat[0];
        document.getElementById("salesStat").innerHTML = stat[1];
        document.getElementById("refundsStat").innerHTML = stat[2];
    }
}

function sendChequeToClient(){
    var cheque = document.getElementById("responseCheque").innerHTML;
    var sendTo = document.getElementById("emailFormInputGroup").value;
    var emailWithChequeList = [];
    emailWithChequeList.push(sendTo);
    emailWithChequeList.push(cheque);
    console.log(emailWithChequeList);
    http.open("POST", url+"/mailcheque", true);
    http.setRequestHeader('Content-Type', 'application/json');
    http.send(JSON.stringify(emailWithChequeList));
    http.onreadystatechange = function () {
        if (this.status != 200) {
            alert("Что-то пошло не так...");
            return;
        }
        var response = JSON.parse(this.responseText);
        if(response.error == "0"){
            alert("Чек успешно отправлен");
            return;
        } else{
            alert("Ошибка отправки чека")
        }
    }
}

function createTransactionObject(){
    var products = document.getElementsByName('prodName');
    var productsAmount = document.getElementsByName('prodAmount');

    if(products.length==productsAmount.length){
        var prodList=[];
        products.forEach(p=>prodList.push(p.textContent));
        var prodAmountList=[];
        productsAmount.forEach(a=>prodAmountList.push(a.textContent));
    }

    var transaction = {
          amount: document.getElementById("totalPrice").textContent,
          productsList: prodList,
          productsAmountList: prodAmountList,
    }

    console.log(transaction);
    return transaction;
}

function getResponseFromServerWithCheque(buttonId){

    document.getElementById("jumbotronChequeArea").className = "jumbotron";
    document.getElementById("responseCheque").innerHTML = "Выполняется операция...";

    http.onreadystatechange = function () {

        if (this.readyState != 4) {
            document.getElementById("responseCheque").innerHTML = "Что-то пошло не так...";
            return;
        }
        var transactionResponsed = JSON.parse(this.responseText);
        if(transactionResponsed.status==false){
            document.getElementById(buttonId).innerHTML = "Отказано";
            document.getElementById(buttonId).className = "btn btn-danger";
            document.getElementById("responseCheque").innerHTML = "Что-то пошло не так...";
        } else {
            document.getElementById(buttonId).innerHTML = "Одобрено" ;
            document.getElementById(buttonId).className = "btn btn-success";
            clearProductsCart();
            document.getElementById("responseCheque").innerHTML = transactionResponsed.cheque;
            document.getElementById("responseCheque").addEventListener("click", printCheque);
        }
    }
}

function clearProductsCart(){
    var productsInCart = document.getElementsByName("productInCart");
    while(productsInCart.length>0){
        productsInCart.forEach(e=>e.remove());
    }
    var products = document.getElementsByName('prodName');
    products.forEach(p=>p.innerText="");
    var productsAmount = document.getElementsByName('prodAmount');
    productsAmount.forEach(a=>a.innerText="");
    document.getElementById("totalPrice").textContent="";
}

function printCheque() {
    var allElements = document.body.innerHTML;
    var cheque = document.getElementById("responseCheque").innerHTML;

    document.body.innerHTML = cheque.replaceAll("\n", "<br>");
    window.print();
    document.body.innerHTML = allElements;

    document.getElementById("responseCheque").addEventListener("click", printCheque);
}

function verifyCartEmpty(){
    var productsInCart = document.getElementsByName("productInCart");
    return productsInCart.length==0;
}

function reloadPage(){
    window.location.reload();
}
