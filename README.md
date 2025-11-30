Small cli tool to convert a text files with Swift MT messages into json files for easier processing

## Installation
To install this package you can use nix with flakes

```nix
{
    inputs = {
        swift-to-json.url = "github:dj-denis/swift-to-json";
        ...
    };
    outputs = {
        nixpkgs,
        swift-to-json,
    }: let {
        system = "...";
    } in {
        systemPackages = [
            swift-to-json.packages.${system}.default
        ];
        ...
    };
}
```

## Cachix
If you want to use cached builds to not build the app yourself you can use cachix

```nix
extra-substituters = [ "https://swift-to-json.cachix.org" ];
extra-trusted-public-keys = [ "swift-to-json.cachix.org-1:/qR0qG81j01/SWs4VsLNJNNbh1pYid7IuC+LnBKO7Z8=" ];
```

Note: do not override nixpkgs input for the swift-to-json, otherwise cache will most likely miss


## Usage

```bash
swift-to-json path/to/dest/*.<ext>
```

This will create json files for each input file
Can be called with one or multiple globs or files
