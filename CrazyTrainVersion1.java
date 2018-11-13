package projetoIA;
import robocode.*;


import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.Condition;
import robocode.util.Utils;

/**
 * Robô implementado para a disciplina Inteligência Artificial UFCG 2018.2
 * Autores: Hebert, Hector, Icaro, Lucas, Rafaela, Yago
 */
public class CrazyTrainVersion1 extends AdvancedRobot {
    
    // Seta poder de fogo da arma
    private double poderDeFogo = 2.0;
    private double aux_poderDeFogo = 2.0;
    private static double direcaoLateral;
    private static double velocidadeInimigo;
    private static Movimento movimento;

    public CrazyTrainVersion1() {
	    //Cria movimento do nosso robô
        movimento = new Movimento(this);
    }

    public void run() {
	    //seta direção do robo
        direcaoLateral = 1;
        //seta velocidade inicial do inimigo
        velocidadeInimigo = 0;
		//desajusta arma e radar do robo, de modo que não gire de acordo com o robo
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        while (true) {
		    //gira arma para direita em Double.POSITIVE_INFINITY radianos
		    System.out.println("Rodou radar");
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }

    }

    public void onScannedRobot(ScannedRobotEvent e) {
		poderDeFogo = aux_poderDeFogo;
		// angulo do inimigo em relação ao robo
        double rolamentoInimigo = getHeadingRadians() + e.getBearingRadians();
        double distanciaInimigo = e.getDistance();
        double velocidadeInimigoTemp = e.getVelocity();
        if (velocidadeInimigoTemp != 0) {
		    //seta direção do robo
            direcaoLateral = BotUtils.sinalDirecao(velocidadeInimigoTemp * Math.sin(e.getHeadingRadians() - rolamentoInimigo));
     	    System.out.println("Direção lateral =");
			System.out.println(direcaoLateral);
        }
        AtaqueEmOnda onda = new AtaqueEmOnda(this);
		//seta local do nosso robo
        onda.setLocalDoRobo(new Point2D.Double(getX(), getY()));
		//seta local do inimigo
        onda.setLocalDoAlvo(BotUtils.projetarDestino(onda.getLocalDoRobo(), rolamentoInimigo, distanciaInimigo));
        onda.setDirecaoLateral(direcaoLateral);
        onda.setPoderDeTiro(poderDeFogo);
        onda.setDadosInimigo(distanciaInimigo, velocidadeInimigoTemp, velocidadeInimigo);
        velocidadeInimigo = velocidadeInimigoTemp;
        onda.setRolamento(rolamentoInimigo);
        setTurnGunRightRadians(Utils.normalRelativeAngle(rolamentoInimigo - getGunHeadingRadians() + onda.mostVisitedBearingOffset()));
        setFire(onda.getPoderDeTiro());
		
		//Se a energia for maior que o poder de fogo, ele atira, caso contrario, só terá direito em tese a mais um tiro.
        if (getEnergy() > poderDeFogo) {
            addCustomEvent(onda);
        } else {
			while (getEnergy() < poderDeFogo) {
				poderDeFogo -= 0.1;	
			}
			onda.setPoderDeTiro(poderDeFogo);
			setFire(onda.getPoderDeTiro());
	 	    addCustomEvent(onda);
		}
		
		//Realiza o movimento em torno do inimigo
        movimento.onScannedRobot(e);
        setTurnRadarRightRadians(Utils.normalRelativeAngle(rolamentoInimigo - getRadarHeadingRadians()) * 2);
    }
	
	public void onHitRobot(HitRobotEvent event) {
 	   int num_inimigos = getOthers();
	   if(num_inimigos < 2){
	      aux_poderDeFogo = 7;
	   }
       if (event.getBearing() > -90 && event.getBearing() <= 90 && num_inimigos >= 2) {
    	    System.out.println("Colidiu no 90");
    	    System.out.println(event.getBearing());
            back(100);
            turnRadarRightRadians(0.8);
       } else if(num_inimigos >= 2) {
			ahead(100);
            turnRadarRightRadians(0.8);
       	    System.out.println("Colidiu fora dos 90");
      	    System.out.println(event.getBearing());
       }
   }
}

class AtaqueEmOnda extends Condition{

    private static final double DISTANCIA_MAXIMA = 1000;
    private static final int DISTANCIAS = 5;
    private static final int VELOCIDADES = 5;
    private static final double ANGULO_DE_FUGA = 0.7;
    private static final int BINS = 45;
    private static final int MIDDLE_BIN = (BINS - 1) / 2;
    
    private static final double BIN_WIDTH = ANGULO_DE_FUGA / (double) MIDDLE_BIN;

    private static int[][][][] dadosInimigos = new int[DISTANCIAS][VELOCIDADES][VELOCIDADES][BINS];

    private static Point2D localDoAlvo;
    private double poderDeTiro;
    private Point2D localDaArma;
    private double rolamento;
    private double direcaoLateral;
    private int[] dados;
    private AdvancedRobot robot;
    private double distanciaPercorrida;

    public AtaqueEmOnda(AdvancedRobot robot) {
        this.robot = robot;
    }

    @Override
    public boolean test() {
        avance();
        if (chegouNoDestino()) {
            dados[binAtual()]++;
            robot.removeCustomEvent(this);
        }
        return false;
    }

    public double mostVisitedBearingOffset() {
        return (direcaoLateral * BIN_WIDTH) * (BinMaisChecado() - MIDDLE_BIN);
    }

    public void setDadosInimigo(double distancia, double velocidade, double ultimaVelocidade) {
        int iDist = (int) (distancia / (DISTANCIA_MAXIMA / DISTANCIAS));
        int iVeloc = (int) Math.abs(velocidade / 2);
        int iUltVeloc = (int) Math.abs(ultimaVelocidade / 2);
        dados = dadosInimigos[iDist][iVeloc][iUltVeloc];
    }

    private void avance() {
        distanciaPercorrida += BotUtils.velocidadeDoTiro(poderDeTiro);
    }

    private boolean chegouNoDestino() {
        return distanciaPercorrida > localDaArma.distance(localDoAlvo) - 18;
    }

    private int binAtual() {
        int bin = (int) Math.round(((Utils.normalRelativeAngle(BotUtils.mira(localDaArma, localDoAlvo) - rolamento))
                / (direcaoLateral * BIN_WIDTH)) + MIDDLE_BIN);
        return BotUtils.minMax(bin, 0, BINS - 1);
    }

    private int BinMaisChecado() {
        int mostVisited = MIDDLE_BIN;
        for (int i = 0; i < BINS; i++) {
            if (dados[i] > dados[mostVisited]) {
                mostVisited = i;
            }
        }
        return mostVisited;
    }

    public static Point2D getLocalDoAlvo() {
        return localDoAlvo;
    }

    public static void setLocalDoAlvo(Point2D localDoAlvo) {
        AtaqueEmOnda.localDoAlvo = localDoAlvo;
    }

    public double getPoderDeTiro() {
        return poderDeTiro;
    }

    public void setPoderDeTiro(double poderDeTiro) {
        this.poderDeTiro = poderDeTiro;
    }

    public Point2D getLocalDoRobo() {
        return localDaArma;
    }

    public void setLocalDoRobo(Point2D localDaArma) {
        this.localDaArma = localDaArma;
    }

    public double getRolamento() {
        return rolamento;
    }

    public void setRolamento(double rolamento) {
        this.rolamento = rolamento;
    }

    public double getDirecaoLateral() {
        return direcaoLateral;
    }

    public void setDirecaoLateral(double direcaoLateral) {
        this.direcaoLateral = direcaoLateral;
    }

    public AdvancedRobot getRobot() {
        return robot;
    }

    public void setRobot(AdvancedRobot robot) {
        this.robot = robot;
    }

    public double getDistanciaPercorrida() {
        return distanciaPercorrida;
    }

    public void setDistanciaPercorrida(double distanciaPercorrida) {
        this.distanciaPercorrida = distanciaPercorrida;
    }
}
class Movimento {

    private static final double LARGURA_CAMPO = 800;
    private static final double ALTURA_CAMPO = 600;
    private static final double MARGEM_PAREDE = 18;
    private static final double TENTATIVAS = 125;
    private static final double FUGA_PADRAO = 1.2;
    private static final double REVERSE_TUNER = 0.421075;
    private static final double WALL_BOUNCE_TUNER = 0.699484;

    private AdvancedRobot robot;
    private Rectangle2D retanguloCampo = new Rectangle2D.Double(MARGEM_PAREDE, MARGEM_PAREDE,
            LARGURA_CAMPO - MARGEM_PAREDE * 2, ALTURA_CAMPO - MARGEM_PAREDE * 2);
    private double poderDeFogoInimigo = 3;
    private double direcao = 0.8;

    public Movimento(AdvancedRobot _robot) {
        this.robot = _robot;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double rolamentoInimigo = robot.getHeadingRadians() + e.getBearingRadians();
        double distanciaInimigo = e.getDistance();
        Point2D localDoRobo = new Point2D.Double(robot.getX(), robot.getY());
        Point2D localDoInimigo = BotUtils.projetarDestino(localDoRobo, rolamentoInimigo, distanciaInimigo);
		System.out.println("local do robo");
    	System.out.println(localDoRobo);
		System.out.println("localDoInimigo");
		System.out.println(localDoInimigo);
        Point2D destinoDoRobo;
        double tentativas = 0;
        while (!retanguloCampo.contains(destinoDoRobo = BotUtils.projetarDestino(localDoInimigo, rolamentoInimigo + Math.PI + direcao,
                distanciaInimigo * (FUGA_PADRAO - tentativas / 100.0))) && tentativas < TENTATIVAS) {
            tentativas++;
        }
		System.out.println("retangulo");
    		System.out.println(retanguloCampo.toString());	
        if ((Math.random() < (BotUtils.velocidadeDoTiro(poderDeFogoInimigo) / REVERSE_TUNER) / distanciaInimigo
                || tentativas > (distanciaInimigo / BotUtils.velocidadeDoTiro(poderDeFogoInimigo) / WALL_BOUNCE_TUNER))) {
            direcao = -direcao;
        }
 
        double angle = BotUtils.mira(localDoRobo, destinoDoRobo) - robot.getHeadingRadians();
        robot.setAhead(Math.cos(angle) * 100);
        robot.setTurnRightRadians(Math.tan(angle));
    }
}
class BotUtils {

    public static double velocidadeDoTiro(double poderDeFogo) {
        return 20 - 3 * poderDeFogo;
    }

    public static Point2D projetarDestino(Point2D localDoBot, double anglo, double dist) {
        return new Point2D.Double(localDoBot.getX() + Math.sin(anglo) * dist,
                localDoBot.getY() + Math.cos(anglo) * dist);
    }

    public static double mira(Point2D bot, Point2D inimigo) {
        return Math.atan2(inimigo.getX() - bot.getX(), inimigo.getY() - bot.getY());
    }

    public static int sinalDirecao(double v) {
        return v < 0 ? -1 : 1;
    }

    public static int minMax(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

}