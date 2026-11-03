# hubitat_MolSmart_Dimmer

Módulos Dimmer da MolSmart
6CH in 
6CH Out. 

Só precisa incluir ele como Device.  
Colocar o IP dentro das configurações.  
O driver vai criar 6 canais.  

## TecnoSimples — atualização / update

### Português

Este driver é mantido pela **TecnoSimples** a partir do código original de Vartan Horigian (VH / TRATO), sob a licença Apache 2.0. O nome, o namespace `TRATO` e os IDs do pacote são os mesmos de sempre: quem instalou por aqui recebe a versão nova pelo **HPM → Update**, sem trocar o Type de nenhum device.

**Como atualizar**
1. Atualize o próprio HPM antes (Hubitat Package Manager 1.9.16 ou mais novo).
2. HPM → **Update** → marque este pacote → aguarde a página dizer "complete" antes de sair dela.
3. Abra cada dimmer e clique **Save Preferences** uma vez: o IP migra para os campos novos e o `driverVersion` aparece. Até lá a tela mostra o IP em branco, mas o dimmer segue funcionando.

**Como saber de onde veio o driver:** o atributo `driverVersion` do device mostra `TS-3.0.0-VH` quando o código foi baixado deste repositório (`TS-3.0.0`, sem `-VH`, é o mesmo código pelo repositório da TecnoSimples).

**Atenção**
- Dashboards e regras que liam `boardstatus` passam a ler `online` (`true`/`false`).
- Saem os comandos `buscainputcount`, `createchilds`, `connectionCheck`, `ManualKeepAlive` e `verstatus`.
- O filho muda pelo retorno da placa (cerca de 50 ms depois do comando), não na hora do clique; a duração do `setLevel` é ignorada.
- Os filhos `-Switch-N` são mantidos: nenhum é apagado nem recriado.

### English

This driver is maintained by **TecnoSimples**, based on the original code by Vartan Horigian (VH / TRATO), under the Apache 2.0 license. The name, the `TRATO` namespace and the package IDs are unchanged: if you installed from here, you get the new version through **HPM → Update**, without changing any device's Type.

**How to update**
1. Update HPM itself first (Hubitat Package Manager 1.9.16 or newer).
2. HPM → **Update** → tick this package → wait for the page to say "complete" before leaving it.
3. Open each dimmer and click **Save Preferences** once: the IP moves to the new fields and `driverVersion` shows up. Until then the IP field looks blank, but the dimmer keeps working.

**Where did the driver come from:** the device's `driverVersion` attribute shows `TS-3.0.0-VH` when the code was downloaded from this repository (`TS-3.0.0`, without `-VH`, is the same code from the TecnoSimples repository).

**Heads-up**
- Dashboards and rules that read `boardstatus` now read `online` (`true`/`false`).
- The commands `buscainputcount`, `createchilds`, `connectionCheck`, `ManualKeepAlive` and `verstatus` are gone.
- A child changes from the board's reply (about 50 ms after the command), not at click time; the `setLevel` duration is ignored.
- `-Switch-N` children are kept: none is deleted or recreated.
