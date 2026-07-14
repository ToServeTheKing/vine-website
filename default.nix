let
  pkgs = import <nixpkgs> {};
in

pkgs.mkShell {
  buildInputs = [
    pkgs.nodejs
  ];

  shellHook = ''
    if ! [ -d "node_modules" ]; then
      npm install --legacy-peer-deps
    fi
  '';
}
