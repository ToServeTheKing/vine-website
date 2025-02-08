{ pkgs ? import <nixpkgs> {} }:

let
    nodePackages = pkgs.nodePackages;
in
pkgs.mkShell {
    buildInputs = [
        nodePackages.nodejs
        nodePackages.yarn
    ];

    shellHook = ''
        echo "Welcome to the bakery-template development environment!"
    '';
}