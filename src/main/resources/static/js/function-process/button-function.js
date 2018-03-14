//邀请群聊
function invite(btn) {
    var list = null;
    var body = document.body;
    var div = document.createElement("div");
    var form = document.createElement("form");
    form.setAttribute("name", "listForm");
    $.ajax({
        url: "/getProfessorList",
        type: "POST",
        data: {
            "user_id": user_id,
        },
        async: false,
        success: function (result) {
            list = result;
        }
    });
    div.setAttribute("id", "list");
    //写成一行
    $.each(list, function (i, t) {
        var p = document.createElement("p");
        var checkbox = document.createElement("input");
        checkbox.setAttribute("type", "checkbox");
        checkbox.setAttribute("value", t);
        checkbox.setAttribute("name", "professor");
        p.appendChild(checkbox);
        p.appendChild(document.createTextNode(t));
        form.appendChild(p);
    });
    var button1 = document.createElement("button");
    button1.innerHTML = "confirm";
    button1.onclick = function (ev) {
        var txt = '';
        var professors = document.listForm.professor;
        for (var i = 0; i < professors.length; i++)
            if (professors[i].checked)
                txt += professors[i].value + ";";
        cancel(document.getElementById("list"));
        var to = txt + user_id + ";" + btn.parentNode.children[4].innerHTML;
        var talk_id = new Date().format("yyyyMMddhhmmss") + "-" + btn.parentNode.children[4].innerHTML;
        var obj = {to: to, mode: "1", talk_id: talk_id};
        websocket.send(JSON.stringify(obj));
    }
    var button2 = document.createElement("button");
    button2.setAttribute("onclick", "cancel(document.getElementById(\"list\"))");
    button2.innerHTML = "cancel";
    div.appendChild(form);
    div.appendChild(button1);
    div.appendChild(button2);
    body.appendChild(div);
}

//退出群聊
function exitGUI(bodyDiv) {
    var exitButton = document.createElement("button");
    exitButton.setAttribute("id", "exit");
    exitButton.setAttribute("onclick", "exit()");
    exitButton.innerHTML = "exit";
    exitButton.onclick = function (ev) {
        var obj = {user_id: user_id, role: role, talk_id: this.parentNode.attributes[0].value, mode: "3"};
        if (role == "professor") {
            cancel(this.parentNode);
        }
        else {
            this.parentNode.attributes[0].value = privateChatObj;
            document.getElementById("to").innerHTML = privateChatObj;
            document.getElementById("mode").innerHTML = "单聊";
            setMessageInnerHTML("您已退出群聊");
            cancel(this);
        }
        websocket.send(JSON.stringify(obj));
    }
    bodyDiv.appendChild(exitButton);
}

//取消邀请框
function cancel(element) {
    element.remove();
}

//发送消息
function send(button) {
    var obj = null;
    var to = null;
    var input = null;
    var nowMode = null;
    var talk_id = null;
    if (role == "common") {
        to = document.getElementById('to').innerHTML;
        input = document.getElementById('text');
        nowMode = document.getElementById('mode').innerHTML;
    } else if (role == "professor") {
        to = button.parentNode.children[4].innerHTML;
        input = button.parentNode.children[0];
        nowMode = button.parentNode.children[5].innerHTML;
    }
    if (nowMode == "单聊")
        nowMode = "0";
    else {
        nowMode = "2";
        talk_id = button.parentNode.attributes[0].value;
    }
    var message = input.value;
    input.value = "";
    obj = {message: message, role: role, user_id: user_id, name: name, to: to, mode: nowMode, talk_id: talk_id};
    websocket.send(JSON.stringify(obj));
}