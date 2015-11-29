package user.totekp.pure

// #CodeCoverage 100%
object StatsMath  {

  def log(in: Double, base: Int): Double = {
    math.log(in) / math.log(base)
  }

  def ln(in: Double): Double = {
    math.log(in)
  }

  def stdevP(values: Seq[Double]): Double = {
    val avg: Double = average(values)
    math.sqrt(values.reduceLeft((acc, v) => acc + math.pow(v - avg, 2)) / values.size)
  }

  def average(values: Seq[Double]): Double = {
    if (values.isEmpty) {
      throw new Exception("Size must be > 0")
    }
    values.sum / values.length
  }

  def standardize(x: Double, mean: Double, sd: Double): Double = {
    (x - mean) / sd
  }

  def entropyFromProbabilities(ps: Seq[Double]): Double = {
    val validPs = validProbabilities(ps)
    validPs.reduceLeft((acc, a) => acc + (- a * log(a, 2)))
  }

  def entropyFromFrequencies(fs: Seq[Double]): Double = {
    val sum = fs.sum
    val ps = fs.map(_ / sum)
    entropyFromProbabilities(ps)
  }

  def validProbabilities(ps: Seq[Double]): Seq[Double] = {
    if (ps.sum.round != 1d)
      throw new Exception("Probabilities do not sum to 1")

    if (ps.min < 0)
      throw new Exception("Negative probability")
    ps.filter(_ > 0)
  }

  // uniform funs
  def getBinaryDigitsPerSymbolUniform(sampleSize: Double): Double = {
    StatsMath.log(sampleSize, 2).ceil
  }

  def getRateFromUniform(channelCapacity: Double, sampleSize: Double, numChained: Double): Double = {
    // (binary digits/symbol)
    val LL = getBinaryDigitsPerSymbolUniform(math.pow(sampleSize, numChained)) / numChained
    // actual communication rate (symbol/s)
    val RR = channelCapacity / LL
    RR
  }

  def getEfficiencyFromUniform(sampleSize: Double, numChained: Double): Double = {
    // entropy (bits/symbol)
    val HH = StatsMath.log(sampleSize, 2)
    // (binary digits/symbol)
    val LL = getBinaryDigitsPerSymbolUniform(math.pow(sampleSize, numChained)) / numChained
    // Coding efficiency (bits/binary digit)
    val EE = HH / LL
    EE
  }
}

// #CodeCoverage 100%
case class ConfusionMatrix2(testP: Double, actualP: Double, trueP: Double) {
  import StatsMath._
  val testN: Double = 1d - testP
  val actualN: Double = 1d - actualP

  /**
    * actual Positive
    */
  val aa: Double = actualP

  /**
    * actual Negative
    */
  val bb: Double = actualN

  /**
    * test Positive
    */
  val cc: Double = testP

  /**
    * test Negative
    */
  val dd: Double = testN

  /**
    * true Positive
    */
  val ee: Double = trueP

  /**
    * false Negative
    */
  val ff: Double = aa - ee

  val falseN: Double = ff

  /**
    * false Positive
    */
  val gg: Double = cc - ee

  val falseP: Double = gg

  /**
    * true Negative
    */
  val hh: Double = bb - gg

  val trueN: Double = hh

  // conditional properties
  val true_P_rate: Double = ee / aa
  val false_N_rate: Double = ff / aa
  val false_P_rate: Double = gg / bb
  val true_N_rate: Double = hh / bb

  /**
    * Positive predictive value
    * p("+" | Test POS)
    */
  val ppv: Double = ee / cc

  /**
    * Negative predictive value
    * p("-" | Test NEG)
    */
  val npv: Double = hh / dd

  /**
    * Definition of Independence P(X,Y) = P(X)p(Y)
    */
  val isIndependent: Boolean = {
    // test TP
    val ac = this.aa * this.cc
    ac == ee
  }

  // information metrics

  /**
    * H(X) 	= a*log(1/a)	+ b*log(1/b)
    * 0.7219	0.4644	0.2575
    */
  def H_x: Double = {
    entropyFromProbabilities(Vector(aa, bb))
  }

  /**
    * H(Y) 	= c*log(1/c)	 + d*log(1/d)
    * 0.8813	0.5211	0.3602
    */
  def H_y: Double = {
    entropyFromProbabilities(Vector(cc, dd))
  }

  /**
    * H(X,Y)	= e*log(1/e)	+ f*log(1/f)	+ g*Log(1/g)	+ h*log(1/h)
    * 1.5710	0.3322	0.3322	0.4644	0.4422
    */
  def H_xy: Double = {
    entropyFromProbabilities(Vector(ee, ff, gg, hh))
  }

  /**
    * Mutual Information I(X:Y) = Relative Entropy of Joint and Product Distributions ---
    * D(p(X,Y||p(X)p(Y)) = e*log(e/ac) 	0.073696559	+ f*log(f/ad)	-0.048542683	+ g*log(g/bc)	-0.052606881	+ h*log(h/bd)	0.059721404
    */
  def KL_divergence: Double = {
    ee * log(ee / (aa * cc), 2) + ff * log(ff/(aa*dd), 2) + gg * log(gg/(bb*cc), 2) + hh * log(hh/(bb*dd), 2)
  }

  /**
    * H(Y|X)	=                  (a	*H(e/a, f/a)) 	+	                   (b	*H(g/b, h/b)
    * 0.2000	1.0000		0.8000	0.8113
    */
  def H_y_given_x: Double = {
    aa * entropyFromProbabilities(Seq(true_P_rate, false_N_rate)) + bb * entropyFromProbabilities(Seq(false_P_rate, true_N_rate))
  }

  /**
    * H(X|Y)	=                  (c	*H(e/c, g/c)	+ 	                  (d	*H(f/d, h/d)
    * 0.3000	0.9183		0.7000	0.5917
    * 0.6897
    */
  def H_x_given_y: Double = {
    cc * entropyFromProbabilities(Seq(ee/cc, gg/cc)) + dd * entropyFromProbabilities(Seq(ff/dd, hh/dd))
  }

  /**
    * I(X;Y) =  	H(X) 	- H(X|Y)
    * 0.0323	0.7219	0.6897
    *
    * I(X;Y) = 	H(Y) 	 - H(Y|X)
    * 0.0323	0.8813	0.8490
    *
    * I(X;Y) = 	H(X) 	+ H(Y) 	 - H(X,Y)
    * 0.0323	0.7219	0.8813	1.5710
    */
  def I_xy: Double = {
    H_x - H_x_given_y
  }

}

