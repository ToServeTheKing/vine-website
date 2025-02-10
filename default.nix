let
  pkgs = import <nixpkgs> {};
in

pkgs.mkShell {
  buildInputs = [
    pkgs.nodejs
    pkgs.yarn
  ];

  # Install Next.js via yarn in the shell
  shellHook = ''
    if ! [ -d "node_modules" ]; then
      yarn add next react react-dom
    fi
  '';
}