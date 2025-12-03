import os

from flask import Flask, jsonify

app = Flask(__name__)


@app.route("/", methods=["GET"])
def get_tasks():
    return jsonify({"message": "Hello World"})


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=os.environ.get("PORT", 5000))
