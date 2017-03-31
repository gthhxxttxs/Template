function callNative(data) {
    window.bridge.call(data);
}

function call(path, params) {
    var data = {
        "path" : path,
        "params" : params
    }
    callNative(JSON.stringify(data));
}

function toast(text) {
    call("toast", text);
    return "ttt";
}