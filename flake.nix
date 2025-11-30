{
  description = "CLI app to convert Swift MT messages into json";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (
      system: let
        pkgs = import nixpkgs {inherit system;};

        graal = pkgs.graalvmPackages.graalvm-ce;
      in {
        devShell = pkgs.mkShell {
          buildInputs = [
            pkgs.maven
            graal
          ];
        };

        packages.default = pkgs.maven.buildMavenPackage {
          pname = "swift-to-json";
          version = "1.0.1";

          src = ./.;
          mvnJdk = graal;
          mvnHash = "sha256-v9plzBxmYTSEFcIf1CNsayxxEjnivVLVlH36BXNgGvw=";

          nativeBuildInputs = [
            # graal
            pkgs.makeWrapper
          ];

          buildPhase = ''
            mvn -e -B -X -DskipTests package
          '';

          installPhase = ''
            mkdir -p $out/bin

            # Copy native image if produced
            if [ -f target/swift-to-json ]; then
              cp target/swift-to-json $out/bin/
            else
              # fallback to jar
              cp target/*-shaded.jar $out/bin/swift-to-json.jar
            fi
          '';
        };
      }
    );
}
