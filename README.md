# SDIS

Como funciona o reclaim?

O valor que se passa é o valor que se quer garantir de memória disponivel? Ou é o valor que deve ser retirado à memoria actual ocupada? Ou ainda o novo limite da memoria ocupada?

Na situação de 2 peers, em que um fez o backup e o outro fez store do chunk, quando o que fez store recebe o RECLAIM e apaga esse chunk, o que deve acontecer? Porque nesta situação ele lança o REMOVE mas o que fez backup como não tem nada stored, ignora
