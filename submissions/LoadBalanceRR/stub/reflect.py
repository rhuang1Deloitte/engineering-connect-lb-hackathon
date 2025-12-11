#!/usr/bin/env python3
# Reflects the requests from HTTP methods GET, POST, PUT, and DELETE
# Written by Nathan Hamiel (2010)
# Modernized for Python 3.10+

from http.server import HTTPServer, BaseHTTPRequestHandler
from argparse import ArgumentParser

class RequestHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        request_path = self.path
        self.send_response(200)
        self.send_header("Set-Cookie", "foo=bar")
        self.end_headers()
        
    def do_POST(self):
        request_path = self.path
        self.send_response(200)
        self.end_headers()
    
    do_PUT = do_POST
    do_DELETE = do_GET
        
def main(port=8080):
    print('Listening on 0.0.0.0:%s' % port)
    server = HTTPServer(('0.0.0.0', port), RequestHandler)
    server.serve_forever()

        
if __name__ == "__main__":
    parser = ArgumentParser(
        description="Creates an HTTP server that echoes out GET and POST parameters"
    )
    parser.add_argument(
        "-p", "--port",
        type=int,
        default=8080,
        help="Port to listen on (default: 8080)"
    )
    args = parser.parse_args()
    
    main(port=args.port)