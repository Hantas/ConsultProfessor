//专家用户界面
function professorFace(user_id) {
    var body = document.body;
    var div = document.createElement("div");
    div.setAttribute("id", user_id);
    div.style.backgroundColor = "red";
    body.appendChild(div);
    clientFace(div, user_id);
}

//非专家用户界面
function clientFace(body, user_id) {

    var button3 = null;

    var text = document.createElement("input");
    text.setAttribute("id", "text");
    text.setAttribute("type", "text");

    var button1 = document.createElement("button");
    button1.setAttribute("onclick", "send(this)");
    button1.innerHTML = "send";
    var button2 = document.createElement("button");
    button2.setAttribute("onclick", "closeWebSocket()");
    button2.innerHTML = "close";
    if (role == "professor") {
        button3 = document.createElement("button");
        button3.setAttribute("onclick", "invite(this)");
        button3.innerHTML = "invite";
    }

    var span = document.createElement("span");
    span.setAttribute("id", "to");
    if (user_id == null) {
        span.innerHTML = privateChatObj;
    } else {
        span.innerHTML = user_id;
    }
    var span2 = document.createElement("span");
    span2.setAttribute("id", "mode");
    span2.innerHTML = "单聊"
    var div = document.createElement("div");
    div.setAttribute("id", "message");

    body.appendChild(text);
    body.appendChild(button1);
    body.appendChild(button2);
    if (button3 != null)
        body.appendChild(button3);
    body.appendChild(span);
    body.appendChild(span2);
    body.appendChild(div);
}