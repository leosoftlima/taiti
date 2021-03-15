public class CalculadoraBasica {

    public int somar(int x, int y){
        return x+y;
    }

    public int subtrair(int x, int y){
        return x-y;
    }

    public void calcular(int x, int y){
        int soma = this.somar(x,y);
        int subtracao = subtrair(x,y);
        System.out.println("Soma: "+soma);
        System.out.println("Subtração: "+subtracao);

        Scanner scanner = new Scanner(System.in);
        System.out.println(Scanner.class);
    }

}